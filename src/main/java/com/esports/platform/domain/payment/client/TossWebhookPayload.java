package com.esports.platform.domain.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossWebhookPayload(
        String eventType,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String paymentKey,
            String orderId,
            String status
    ) {
    }
}
