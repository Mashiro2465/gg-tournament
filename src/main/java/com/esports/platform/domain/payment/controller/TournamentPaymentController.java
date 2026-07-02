package com.esports.platform.domain.payment.controller;

import com.esports.platform.domain.payment.dto.PaymentResponse;
import com.esports.platform.domain.payment.entity.Payment;
import com.esports.platform.domain.payment.service.PaymentService;
import com.esports.platform.global.auth.UserPrincipal;
import com.esports.platform.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/payments")
@RequiredArgsConstructor
public class TournamentPaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> prepare(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tournamentId
    ) {
        Payment payment = paymentService.preparePayment(tournamentId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment), "결제 준비가 완료되었습니다"));
    }
}
