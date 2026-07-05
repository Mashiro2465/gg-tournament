package com.esports.platform.domain.payment.client;

import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class TossPaymentClient {

    private static final String BASE_URL = "https://api.tosspayments.com";

    private final RestClient restClient;

    public TossPaymentClient(@Value("${toss.secret-key}") String secretKey) {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeaders(headers -> headers.setBasicAuth(secretKey, ""))
                .build();
    }

    public TossConfirmResponse confirm(String paymentKey, String orderId, BigDecimal amount) {
        try {
            return restClient.post()
                    .uri("/v1/payments/confirm")
                    .body(new ConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossConfirmResponse.class);
        } catch (RestClientResponseException e) {
            log.error("토스페이먼츠 결제 승인 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }
    }

    public TossCancelResponse cancel(String paymentKey, String cancelReason) {
        try {
            return restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .body(new CancelRequest(cancelReason))
                    .retrieve()
                    .body(TossCancelResponse.class);
        } catch (RestClientResponseException e) {
            log.error("토스페이먼츠 결제 취소 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    private record ConfirmRequest(String paymentKey, String orderId, BigDecimal amount) {
    }

    private record CancelRequest(String cancelReason) {
    }
}
