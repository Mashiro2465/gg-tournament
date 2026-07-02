package com.esports.platform.domain.payment.service;

import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.payment.entity.Payment;
import com.esports.platform.domain.payment.repository.PaymentRepository;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentService가 토스페이먼츠 API를 호출하는 동안에는 DB 트랜잭션을 열어두지 않도록,
 * 조회/검증과 최종 반영을 짧은 트랜잭션 단위로 분리해 실행하는 빈.
 * 같은 클래스 내 self-invocation은 프록시를 거치지 않아 @Transactional이 적용되지 않으므로
 * 별도 빈으로 둔다(ParticipantJoinExecutor와 동일한 이유).
 */
@Component
@RequiredArgsConstructor
class PaymentExecutor {

    private final PaymentRepository paymentRepository;
    private final TournamentParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public Payment loadForConfirm(Long userId, String orderId, BigDecimal clientAmount) {
        Payment payment = findByOrderIdOrThrow(orderId);
        validateOwner(payment, userId);

        if (!payment.isPending()) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        // 클라이언트가 보낸 금액은 신뢰하지 않고, 결제 준비 시점에 서버가 기록한 금액과만 비교한다.
        if (payment.getAmount().compareTo(clientAmount) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        return payment;
    }

    @Transactional
    public Payment applyConfirm(Long paymentId, String paymentKey) {
        Payment payment = findByIdOrThrow(paymentId);
        payment.confirm(paymentKey);
        confirmParticipant(payment);
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment loadForRefund(Long userId, Long paymentId) {
        Payment payment = findByIdOrThrow(paymentId);
        validateOwner(payment, userId);

        if (!payment.isConfirmed()) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_CONFIRMED);
        }
        return payment;
    }

    @Transactional
    public Payment applyRefund(Long paymentId) {
        Payment payment = findByIdOrThrow(paymentId);
        payment.refund();
        return payment;
    }

    private void confirmParticipant(Payment payment) {
        TournamentParticipant participant = participantRepository
                .findByTournamentIdAndUserId(payment.getTournament().getId(), payment.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
        participant.confirm();
    }

    private Payment findByOrderIdOrThrow(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private Payment findByIdOrThrow(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateOwner(Payment payment, Long userId) {
        if (!payment.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
