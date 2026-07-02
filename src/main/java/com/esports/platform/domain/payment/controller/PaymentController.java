package com.esports.platform.domain.payment.controller;

import com.esports.platform.domain.payment.dto.ConfirmPaymentRequest;
import com.esports.platform.domain.payment.dto.PaymentResponse;
import com.esports.platform.domain.payment.dto.RefundPaymentRequest;
import com.esports.platform.domain.payment.entity.Payment;
import com.esports.platform.domain.payment.service.PaymentService;
import com.esports.platform.global.auth.UserPrincipal;
import com.esports.platform.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirm(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        Payment payment = paymentService.confirmPayment(
                userPrincipal.getId(), request.paymentKey(), request.orderId(), request.amount());
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment), "결제 승인이 완료되었습니다"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestHeader("X-Toss-Signature") String signature,
            @RequestBody String rawPayload
    ) {
        paymentService.handleWebhook(signature, rawPayload);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody RefundPaymentRequest request
    ) {
        Payment payment = paymentService.refund(userPrincipal.getId(), id, request.reason());
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment), "환불이 완료되었습니다"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        Payment payment = paymentService.findById(id, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment)));
    }
}
