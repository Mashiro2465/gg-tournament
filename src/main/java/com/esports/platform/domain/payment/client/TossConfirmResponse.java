package com.esports.platform.domain.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        BigDecimal totalAmount
) {
}
