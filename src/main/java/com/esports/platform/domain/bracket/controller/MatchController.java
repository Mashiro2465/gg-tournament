package com.esports.platform.domain.bracket.controller;

import com.esports.platform.domain.bracket.dto.MatchResponse;
import com.esports.platform.domain.bracket.dto.RecordMatchResultRequest;
import com.esports.platform.domain.bracket.entity.Match;
import com.esports.platform.domain.bracket.service.MatchService;
import com.esports.platform.global.auth.UserPrincipal;
import com.esports.platform.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PutMapping("/{id}/result")
    public ResponseEntity<ApiResponse<MatchResponse>> recordResult(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody RecordMatchResultRequest request
    ) {
        Match match = matchService.recordResult(id, userPrincipal.getId(), request.winnerParticipantId());
        return ResponseEntity.ok(ApiResponse.success(MatchResponse.from(match), "경기 결과가 입력되었습니다"));
    }
}
