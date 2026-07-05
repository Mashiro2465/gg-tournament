package com.esports.platform.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.payment.client.TossCancelResponse;
import com.esports.platform.domain.payment.client.TossConfirmResponse;
import com.esports.platform.domain.payment.client.TossPaymentClient;
import com.esports.platform.domain.payment.entity.Payment;
import com.esports.platform.domain.payment.entity.PaymentStatus;
import com.esports.platform.domain.payment.repository.PaymentRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String WEBHOOK_SECRET_KEY = "test-webhook-secret-key";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TournamentParticipantRepository participantRepository;

    @Mock
    private PaymentExecutor paymentExecutor;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void 결제준비_기존PENDING결제없으면_신규생성() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByTournamentIdAndUserIdAndStatus(1L, 100L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.preparePayment(1L, 100L);

        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(payment.getOrderId()).startsWith("TOURNAMENT_1_100_");
        assertThat(payment.isPending()).isTrue();
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void 결제준비_기존PENDING결제있으면_재사용() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        Payment existingPayment = Payment.create(user, tournament, "TOURNAMENT_1_100_111", tournament.getEntryFee());
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.of(participant));
        when(paymentRepository.findByTournamentIdAndUserIdAndStatus(1L, 100L, PaymentStatus.PENDING))
                .thenReturn(Optional.of(existingPayment));

        Payment payment = paymentService.preparePayment(1L, 100L);

        assertThat(payment).isEqualTo(existingPayment);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void 결제준비_참가정보없으면_예외발생() {
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.preparePayment(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 결제준비_취소된참가면_예외발생() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        participant.cancel();
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> paymentService.preparePayment(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 결제승인_토스승인성공시_확정처리위임() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        Payment pendingPayment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        Payment confirmedPayment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        confirmedPayment.confirm("payment-key-1");
        when(paymentExecutor.loadForConfirm(100L, "order-1", BigDecimal.valueOf(10000))).thenReturn(pendingPayment);
        when(tossPaymentClient.confirm("payment-key-1", "order-1", BigDecimal.valueOf(10000)))
                .thenReturn(new TossConfirmResponse("payment-key-1", "order-1", "DONE", BigDecimal.valueOf(10000)));
        when(paymentExecutor.applyConfirm(pendingPayment.getId(), "payment-key-1")).thenReturn(confirmedPayment);

        Payment result = paymentService.confirmPayment(100L, "payment-key-1", "order-1", BigDecimal.valueOf(10000));

        assertThat(result).isEqualTo(confirmedPayment);
        verify(paymentExecutor).applyConfirm(pendingPayment.getId(), "payment-key-1");
    }

    @Test
    void 결제승인_토스상태가DONE아니면_예외발생() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        Payment pendingPayment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        when(paymentExecutor.loadForConfirm(100L, "order-1", BigDecimal.valueOf(10000))).thenReturn(pendingPayment);
        when(tossPaymentClient.confirm("payment-key-1", "order-1", BigDecimal.valueOf(10000)))
                .thenReturn(new TossConfirmResponse("payment-key-1", "order-1", "IN_PROGRESS", BigDecimal.valueOf(10000)));

        assertThatThrownBy(() -> paymentService.confirmPayment(100L, "payment-key-1", "order-1", BigDecimal.valueOf(10000)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_CONFIRM_FAILED);

        verify(paymentExecutor, never()).applyConfirm(any(), any());
    }

    @Test
    void 웹훅_서명이유효하지않으면_예외발생() {
        ReflectionTestUtils.setField(paymentService, "webhookSecretKey", WEBHOOK_SECRET_KEY);
        String payload = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"paymentKey\":\"pk\",\"orderId\":\"order-1\",\"status\":\"DONE\"}}";

        assertThatThrownBy(() -> paymentService.handleWebhook("invalid-signature", payload))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOSS_SIGNATURE);

        verify(paymentRepository, never()).findByOrderId(any());
    }

    @Test
    void 웹훅_결제완료상태수신시_결제및참가상태확정() {
        ReflectionTestUtils.setField(paymentService, "webhookSecretKey", WEBHOOK_SECRET_KEY);
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        Payment payment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        String payload = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"paymentKey\":\"pk-1\",\"orderId\":\"order-1\",\"status\":\"DONE\"}}";
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.of(participant));

        paymentService.handleWebhook(sign(payload), payload);

        assertThat(payment.isConfirmed()).isTrue();
        assertThat(payment.getPaymentKey()).isEqualTo("pk-1");
        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
    }

    @Test
    void 웹훅_이미처리된결제면_무시() {
        ReflectionTestUtils.setField(paymentService, "webhookSecretKey", WEBHOOK_SECRET_KEY);
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        Payment payment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        payment.confirm("already-confirmed-key");
        String payload = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"paymentKey\":\"pk-1\",\"orderId\":\"order-1\",\"status\":\"DONE\"}}";
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        paymentService.handleWebhook(sign(payload), payload);

        assertThat(payment.getPaymentKey()).isEqualTo("already-confirmed-key");
        verify(participantRepository, never()).findByTournamentIdAndUserId(any(), any());
    }

    @Test
    void 웹훅_결제취소상태수신시_결제취소처리() {
        ReflectionTestUtils.setField(paymentService, "webhookSecretKey", WEBHOOK_SECRET_KEY);
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        Payment payment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        String payload = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\",\"data\":{\"paymentKey\":\"pk-1\",\"orderId\":\"order-1\",\"status\":\"CANCELED\"}}";
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        paymentService.handleWebhook(sign(payload), payload);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void 환불_토스취소성공시_환불처리위임() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        Payment confirmedPayment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        confirmedPayment.confirm("payment-key-1");
        Payment refundedPayment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        refundedPayment.confirm("payment-key-1");
        refundedPayment.refund();
        when(paymentExecutor.loadForRefund(100L, 1L)).thenReturn(confirmedPayment);
        when(tossPaymentClient.cancel("payment-key-1", "단순 변심"))
                .thenReturn(new TossCancelResponse("payment-key-1", "order-1", "CANCELED"));
        when(paymentExecutor.applyRefund(1L)).thenReturn(refundedPayment);

        Payment result = paymentService.refund(100L, 1L, "단순 변심");

        assertThat(result).isEqualTo(refundedPayment);
        verify(paymentExecutor).applyRefund(1L);
    }

    @Test
    void 환불_토스취소실패시_예외발생() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        Payment confirmedPayment = createPayment(user, tournament, "order-1", BigDecimal.valueOf(10000));
        confirmedPayment.confirm("payment-key-1");
        when(paymentExecutor.loadForRefund(100L, 1L)).thenReturn(confirmedPayment);
        when(tossPaymentClient.cancel("payment-key-1", "단순 변심"))
                .thenReturn(new TossCancelResponse("payment-key-1", "order-1", "FAILED"));

        assertThatThrownBy(() -> paymentService.refund(100L, 1L, "단순 변심"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_CANCEL_FAILED);

        verify(paymentExecutor, never()).applyRefund(any());
    }

    @Test
    void 결제조회_소유자아니면_예외발생() {
        User owner = createUser(100L);
        User other = createUser(200L);
        Tournament tournament = createTournament(1L, BigDecimal.valueOf(10000));
        Payment payment = createPayment(owner, tournament, "order-1", BigDecimal.valueOf(10000));
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.findById(1L, other.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void 결제조회_결제없으면_예외발생() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findById(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(computed);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private User createUser(Long id) {
        User user = User.createLocalUser("user" + id + "@example.com", "encoded-password", "참가자" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Tournament createTournament(Long id, BigDecimal entryFee) {
        Tournament tournament = Tournament.create(
                createUser(1L), "대회", "게임", TournamentFormat.SINGLE_ELIMINATION, 16,
                entryFee, "{}", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        ReflectionTestUtils.setField(tournament, "id", id);
        return tournament;
    }

    private Payment createPayment(User user, Tournament tournament, String orderId, BigDecimal amount) {
        Payment payment = Payment.create(user, tournament, orderId, amount);
        ReflectionTestUtils.setField(payment, "id", 1L);
        return payment;
    }
}
