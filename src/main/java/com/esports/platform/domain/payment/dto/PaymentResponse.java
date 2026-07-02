package com.esports.platform.domain.payment.dto;

import com.esports.platform.domain.payment.entity.Payment;
import com.esports.platform.domain.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long tournamentId,
        Long userId,
        String orderId,
        String paymentKey,
        BigDecimal amount,
        PaymentStatus status,
        LocalDateTime paidAt,
        LocalDateTime refundedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getTournament().getId(),
                payment.getUser().getId(),
                payment.getOrderId(),
                payment.getPaymentKey(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getRefundedAt()
        );
    }
}
