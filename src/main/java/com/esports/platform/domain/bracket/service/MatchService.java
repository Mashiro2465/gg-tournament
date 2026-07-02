package com.esports.platform.domain.bracket.service;

import com.esports.platform.domain.bracket.entity.Match;
import com.esports.platform.domain.bracket.repository.MatchRepository;
import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.service.TournamentService;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentService tournamentService;

    // 참가자 셔플(시드 배정) → 2^n 계산 → 부전승 처리 → 1라운드 경기 일괄 생성.
    // 부전승 경기는 생성과 동시에 승자가 확정되므로 곧바로 다음 라운드로 진출시킨다.
    @Transactional
    public List<Match> generateBracket(Long tournamentId, Long hostId) {
        if (matchRepository.existsByTournamentId(tournamentId)) {
            throw new BusinessException(ErrorCode.BRACKET_ALREADY_GENERATED);
        }

        List<TournamentParticipant> participants =
                new ArrayList<>(participantRepository.findByTournamentIdAndStatus(tournamentId, ParticipantStatus.CONFIRMED));
        if (participants.size() < 2) {
            throw new BusinessException(ErrorCode.BRACKET_NOT_ENOUGH_PARTICIPANTS);
        }

        // 호스트 검증 및 대회 상태 전이(RECRUITING → IN_PROGRESS)는 TournamentService에 위임한다.
        tournamentService.start(tournamentId, hostId);
        Tournament tournament = tournamentService.findById(tournamentId);

        Collections.shuffle(participants);
        List<Match> firstRoundMatches = createFirstRoundMatches(tournament, participants);

        for (Match match : firstRoundMatches) {
            if (match.isBye()) {
                advanceWinner(tournament, match);
            }
        }

        return firstRoundMatches;
    }

    @Transactional
    public Match recordResult(Long matchId, Long hostId, Long winnerParticipantId) {
        Match match = findByIdOrThrow(matchId);
        Tournament tournament = match.getTournament();
        if (!tournament.isHost(hostId)) {
            throw new BusinessException(ErrorCode.TOURNAMENT_NOT_HOST);
        }

        TournamentParticipant winner = participantRepository.findById(winnerParticipantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));

        match.recordResult(winner);
        advanceWinner(tournament, match);
        return match;
    }

    public List<Match> findBracket(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(tournamentId);
    }

    private List<Match> createFirstRoundMatches(Tournament tournament, List<TournamentParticipant> participants) {
        int bracketSize = nextPowerOfTwo(participants.size());
        int byeCount = bracketSize - participants.size();
        int nodeCount = bracketSize / 2;

        List<Match> matches = new ArrayList<>(nodeCount);
        int cursor = 0;
        for (int matchNumber = 1; matchNumber <= nodeCount; matchNumber++) {
            if (matchNumber <= byeCount) {
                TournamentParticipant participant = participants.get(cursor++);
                matches.add(matchRepository.save(Match.createBye(tournament, 1, matchNumber, participant)));
            } else {
                TournamentParticipant participant1 = participants.get(cursor++);
                TournamentParticipant participant2 = participants.get(cursor++);
                matches.add(matchRepository.save(
                        Match.createScheduled(tournament, 1, matchNumber, participant1, participant2)));
            }
        }
        return matches;
    }

    private void advanceWinner(Tournament tournament, Match completedMatch) {
        List<Match> currentRoundMatches =
                matchRepository.findByTournamentIdAndRound(tournament.getId(), completedMatch.getRound());
        if (currentRoundMatches.size() == 1) {
            tournament.finish();
            return;
        }

        int nextRound = completedMatch.getRound() + 1;
        int nextMatchNumber = (completedMatch.getMatchNumber() + 1) / 2;
        boolean intoFirstSlot = completedMatch.getMatchNumber() % 2 == 1;
        TournamentParticipant winner = completedMatch.getWinner();

        matchRepository.findByTournamentIdAndRoundAndMatchNumber(tournament.getId(), nextRound, nextMatchNumber)
                .ifPresentOrElse(
                        nextMatch -> nextMatch.assignParticipant(winner, intoFirstSlot),
                        () -> matchRepository.save(createNextRoundMatch(tournament, nextRound, nextMatchNumber, winner, intoFirstSlot))
                );
    }

    private Match createNextRoundMatch(
            Tournament tournament, int round, int matchNumber, TournamentParticipant winner, boolean intoFirstSlot
    ) {
        return intoFirstSlot
                ? Match.createScheduled(tournament, round, matchNumber, winner, null)
                : Match.createScheduled(tournament, round, matchNumber, null, winner);
    }

    private Match findByIdOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }
}
