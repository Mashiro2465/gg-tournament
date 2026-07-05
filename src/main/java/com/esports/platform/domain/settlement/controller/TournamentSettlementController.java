package com.esports.platform.domain.settlement.controller;

import com.esports.platform.domain.settlement.dto.SettlementResponse;
import com.esports.platform.domain.settlement.service.SettlementService;
import com.esports.platform.global.auth.UserPrincipal;
import com.esports.platform.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/settlements")
@RequiredArgsConstructor
public class TournamentSettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SettlementResponse>>> getTournamentSettlements(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tournamentId
    ) {
        List<SettlementResponse> responses = settlementService.findByTournamentId(tournamentId, userPrincipal.getId()).stream()
                .map(SettlementResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
