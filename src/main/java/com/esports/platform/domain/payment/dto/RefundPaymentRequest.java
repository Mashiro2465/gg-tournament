package com.esports.platform.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundPaymentRequest(
        @NotBlank(message = "환불 사유는 필수입니다.")
        String reason
) {
}
