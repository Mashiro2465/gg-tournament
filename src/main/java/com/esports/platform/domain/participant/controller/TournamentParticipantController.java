package com.esports.platform.domain.participant.controller;

import com.esports.platform.domain.participant.dto.ParticipantResponse;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.service.TournamentParticipantService;
import com.esports.platform.global.auth.UserPrincipal;
import com.esports.platform.global.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/participants")
@RequiredArgsConstructor
public class TournamentParticipantController {

    private final TournamentParticipantService participantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParticipantResponse>>> getParticipants(@PathVariable Long tournamentId) {
        List<ParticipantResponse> responses = participantService.findByTournamentId(tournamentId).stream()
                .map(ParticipantResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ParticipantResponse>> join(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tournamentId
    ) {
        TournamentParticipant participant = participantService.join(tournamentId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(ParticipantResponse.from(participant), "참가 신청이 완료되었습니다"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long tournamentId
    ) {
        participantService.cancel(tournamentId, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "참가가 취소되었습니다"));
    }
}
