package com.esports.platform.domain.tournament.controller;

import com.esports.platform.domain.bracket.dto.MatchResponse;
import com.esports.platform.domain.bracket.entity.Match;
import com.esports.platform.domain.bracket.service.MatchService;
import com.esports.platform.domain.tournament.dto.CreateTournamentRequest;
import com.esports.platform.domain.tournament.dto.TournamentDetailResponse;
import com.esports.platform.domain.tournament.dto.TournamentSummaryResponse;
import com.esports.platform.domain.tournament.dto.UpdateTournamentRequest;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.esports.platform.domain.tournament.repository.TournamentSearchCondition;
import com.esports.platform.domain.tournament.service.TournamentService;
import com.esports.platform.global.auth.UserPrincipal;
import com.esports.platform.global.common.ApiResponse;
import com.esports.platform.global.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final MatchService matchService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TournamentSummaryResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gameType,
            @RequestParam(required = false) TournamentFormat format,
            @RequestParam(required = false) TournamentStatus status,
            Pageable pageable
    ) {
        TournamentSearchCondition condition = new TournamentSearchCondition(keyword, gameType, format, status);
        Page<TournamentSummaryResponse> result = tournamentService.search(condition, pageable)
                .map(TournamentSummaryResponse::from);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(result)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TournamentDetailResponse>> getDetail(@PathVariable Long id) {
        Tournament tournament = tournamentService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(TournamentDetailResponse.from(tournament)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TournamentDetailResponse>> create(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateTournamentRequest request
    ) {
        Tournament tournament = tournamentService.create(
                userPrincipal.getId(),
                request.title(),
                request.gameType(),
                request.format(),
                request.maxParticipants(),
                request.entryFee(),
                request.prizeStructure(),
                request.registrationDeadline(),
                request.startAt(),
                request.endAt()
        );
        return ResponseEntity.ok(ApiResponse.success(TournamentDetailResponse.from(tournament), "대회가 생성되었습니다"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TournamentDetailResponse>> update(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTournamentRequest request
    ) {
        tournamentService.update(
                id,
                userPrincipal.getId(),
                request.title(),
                request.gameType(),
                request.maxParticipants(),
                request.prizeStructure(),
                request.registrationDeadline(),
                request.startAt(),
                request.endAt()
        );
        Tournament tournament = tournamentService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(TournamentDetailResponse.from(tournament), "대회가 수정되었습니다"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        tournamentService.cancel(id, userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "대회가 취소되었습니다"));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> start(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id
    ) {
        List<Match> firstRoundMatches = matchService.generateBracket(id, userPrincipal.getId());
        List<MatchResponse> responses = firstRoundMatches.stream().map(MatchResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(responses, "대회가 시작되었습니다"));
    }
}
