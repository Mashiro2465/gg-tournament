package com.esports.platform.domain.bracket.controller;

import com.esports.platform.domain.bracket.dto.MatchResponse;
import com.esports.platform.domain.bracket.service.MatchService;
import com.esports.platform.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/bracket")
@RequiredArgsConstructor
public class TournamentBracketController {

    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getBracket(@PathVariable Long tournamentId) {
        List<MatchResponse> responses = matchService.findBracket(tournamentId).stream()
                .map(MatchResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
