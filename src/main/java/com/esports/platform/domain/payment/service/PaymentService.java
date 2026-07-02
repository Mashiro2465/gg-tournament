package com.esports.platform.domain.payment.service;

import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.payment.client.TossCancelResponse;
import com.esports.platform.domain.payment.client.TossConfirmResponse;
import com.esports.platform.domain.payment.client.TossPaymentClient;
import com.esports.platform.domain.payment.client.TossWebhookPayload;
import com.esports.platform.domain.payment.entity.Payment;
import com.esports.platform.domain.payment.entity.PaymentStatus;
import com.esports.platform.domain.payment.repository.PaymentRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TOSS_STATUS_DONE = "DONE";
    private static final String TOSS_STATUS_CANCELED = "CANCELED";
    private static final String TOSS_HMAC_ALGORITHM = "HmacSHA256";

    private final PaymentRepository paymentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final PaymentExecutor paymentExecutor;
    private final TossPaymentClient tossPaymentClient;
    private final ObjectMapper objectMapper;

    @Value("${toss.secret-key}")
    private String webhookSecretKey;

    @Transactional
    public Payment preparePayment(Long tournamentId, Long userId) {
        TournamentParticipant participant = findParticipantOrThrow(tournamentId, userId);
        if (participant.isCancelled()) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }

        return paymentRepository.findByTournamentIdAndUserIdAndStatus(tournamentId, userId, PaymentStatus.PENDING)
                .orElseGet(() -> createPendingPayment(participant));
    }

    // Toss 승인 API 호출 중에는 DB 트랜잭션을 열어두지 않는다. 조회/검증과 최종 반영은
    // PaymentExecutor의 짧은 트랜잭션으로 각각 분리해 처리한다.
    public Payment confirmPayment(Long userId, String paymentKey, String orderId, BigDecimal clientAmount) {
        Payment payment = paymentExecutor.loadForConfirm(userId, orderId, clientAmount);

        TossConfirmResponse response = tossPaymentClient.confirm(paymentKey, orderId, payment.getAmount());
        if (!TOSS_STATUS_DONE.equals(response.status())) {
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        return paymentExecutor.applyConfirm(payment.getId(), paymentKey);
    }

    @Transactional
    public void handleWebhook(String signatureHeader, String rawPayload) {
        if (!isValidSignature(rawPayload, signatureHeader)) {
            throw new BusinessException(ErrorCode.INVALID_TOSS_SIGNATURE);
        }

        TossWebhookPayload.Data data = parsePayload(rawPayload).data();
        Payment payment = paymentRepository.findByOrderId(data.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 웹훅은 재전송될 수 있으므로, 이미 최종 상태로 반영된 결제는 그대로 무시한다(멱등 처리).
        if (!payment.isPending()) {
            log.info("이미 처리된 결제 웹훅 무시: orderId={}, status={}", data.orderId(), payment.getStatus());
            return;
        }

        if (TOSS_STATUS_DONE.equals(data.status())) {
            payment.confirm(data.paymentKey());
            confirmParticipant(payment);
        } else if (TOSS_STATUS_CANCELED.equals(data.status())) {
            payment.cancel();
        }
    }

    // Toss 취소 API 호출 중에는 DB 트랜잭션을 열어두지 않는다.
    public Payment refund(Long userId, Long paymentId, String reason) {
        Payment payment = paymentExecutor.loadForRefund(userId, paymentId);

        TossCancelResponse response = tossPaymentClient.cancel(payment.getPaymentKey(), reason);
        if (!TOSS_STATUS_CANCELED.equals(response.status())) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        return paymentExecutor.applyRefund(paymentId);
    }

    @Transactional(readOnly = true)
    public Payment findById(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        if (!payment.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> findByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    private Payment createPendingPayment(TournamentParticipant participant) {
        Tournament tournament = participant.getTournament();
        User user = participant.getUser();
        String orderId = generateOrderId(tournament.getId(), user.getId());
        return paymentRepository.save(Payment.create(user, tournament, orderId, tournament.getEntryFee()));
    }

    private void confirmParticipant(Payment payment) {
        TournamentParticipant participant = findParticipantOrThrow(payment.getTournament().getId(), payment.getUser().getId());
        participant.confirm();
    }

    private TournamentParticipant findParticipantOrThrow(Long tournamentId, Long userId) {
        return participantRepository.findByTournamentIdAndUserId(tournamentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private TossWebhookPayload parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, TossWebhookPayload.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private boolean isValidSignature(String rawPayload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(TOSS_HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(webhookSecretKey.getBytes(StandardCharsets.UTF_8), TOSS_HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(computed);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("토스 웹훅 서명 검증 중 오류 발생", e);
            return false;
        }
    }

    private String generateOrderId(Long tournamentId, Long userId) {
        return "TOURNAMENT_%d_%d_%d".formatted(tournamentId, userId, System.currentTimeMillis());
    }
}
