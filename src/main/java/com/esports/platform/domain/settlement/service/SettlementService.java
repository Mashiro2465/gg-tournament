package com.esports.platform.domain.settlement.service;

import com.esports.platform.domain.bracket.entity.Match;
import com.esports.platform.domain.bracket.repository.MatchRepository;
import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.settlement.entity.Settlement;
import com.esports.platform.domain.settlement.entity.SettlementStatus;
import com.esports.platform.domain.settlement.repository.SettlementRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.esports.platform.domain.tournament.repository.TournamentRepository;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private static final BigDecimal PLATFORM_FEE_RATE = BigDecimal.valueOf(0.10);
    private static final BigDecimal PERCENT_DIVISOR = BigDecimal.valueOf(100);

    private final SettlementRepository settlementRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    public List<Tournament> findUnsettledFinishedTournaments() {
        return tournamentRepository.findByStatus(TournamentStatus.FINISHED).stream()
                .filter(tournament -> !settlementRepository.existsByTournamentId(tournament.getId()))
                .toList();
    }

    // 대진표 결과로 순위를 확인하고, 참가비 기반 상금 풀을 순위별 배분율로 나눈 뒤
    // 플랫폼 수수료(10%)를 차감한다. 이 시점에는 저장하지 않고 계산만 한다.
    public List<Settlement> calculateSettlements(Tournament tournament) {
        Map<Integer, TournamentParticipant> rankedParticipants = determineRanks(tournament);
        if (rankedParticipants.isEmpty()) {
            return List.of();
        }

        Map<Integer, Integer> prizeShares = parsePrizeStructure(tournament.getPrizeStructure());
        BigDecimal totalPrizePool = calculateTotalPrizePool(tournament);

        List<Settlement> settlements = new ArrayList<>();
        for (Map.Entry<Integer, TournamentParticipant> entry : rankedParticipants.entrySet()) {
            int rank = entry.getKey();
            Integer sharePercent = prizeShares.get(rank);
            if (sharePercent == null) {
                continue;
            }

            TournamentParticipant participant = entry.getValue();
            BigDecimal grossPrize = totalPrizePool
                    .multiply(BigDecimal.valueOf(sharePercent))
                    .divide(PERCENT_DIVISOR, 2, RoundingMode.DOWN);
            BigDecimal platformFee = grossPrize.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.DOWN);
            BigDecimal netPrize = grossPrize.subtract(platformFee);

            settlements.add(Settlement.create(tournament, participant.getUser(), rank, netPrize, platformFee));
        }
        return settlements;
    }

    @Transactional
    public List<Settlement> saveAll(List<Settlement> settlements) {
        return settlementRepository.saveAll(settlements);
    }

    @Transactional
    public void completeAllPending() {
        settlementRepository.findByStatus(SettlementStatus.PENDING)
                .forEach(Settlement::complete);
    }

    public List<Settlement> findByUserId(Long userId) {
        return settlementRepository.findByUserId(userId);
    }

    public List<Settlement> findByTournamentId(Long tournamentId, Long hostId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOURNAMENT_NOT_FOUND));
        if (!tournament.isHost(hostId)) {
            throw new BusinessException(ErrorCode.TOURNAMENT_NOT_HOST);
        }
        return settlementRepository.findByTournamentId(tournamentId);
    }

    // 결승전 승자를 1위, 패자를 2위로 확정한다. 별도의 3-4위전을 두지 않으므로
    // 준결승 패자 한 명을 공동 3위로 처리한다(prizeStructure에 "3rd"가 정의된 경우에만 지급).
    private Map<Integer, TournamentParticipant> determineRanks(Tournament tournament) {
        List<Match> matches = matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(tournament.getId());
        if (matches.isEmpty()) {
            return Map.of();
        }

        int finalRound = matches.stream().mapToInt(Match::getRound).max().orElseThrow();
        Optional<Match> finalMatch = matches.stream()
                .filter(match -> match.getRound() == finalRound)
                .findFirst();
        if (finalMatch.isEmpty() || !finalMatch.get().isFinished() || finalMatch.get().getWinner() == null) {
            return Map.of();
        }

        Map<Integer, TournamentParticipant> ranks = new LinkedHashMap<>();
        ranks.put(1, finalMatch.get().getWinner());
        resolveLoser(finalMatch.get()).ifPresent(runnerUp -> ranks.put(2, runnerUp));

        int semiFinalRound = finalRound - 1;
        matches.stream()
                .filter(match -> match.getRound() == semiFinalRound && match.isFinished())
                .findFirst()
                .flatMap(this::resolveLoser)
                .ifPresent(thirdPlace -> ranks.put(3, thirdPlace));

        return ranks;
    }

    private Optional<TournamentParticipant> resolveLoser(Match match) {
        TournamentParticipant winner = match.getWinner();
        if (winner == null) {
            return Optional.empty();
        }
        TournamentParticipant participant1 = match.getParticipant1();
        TournamentParticipant participant2 = match.getParticipant2();
        if (participant1 != null && !participant1.getId().equals(winner.getId())) {
            return Optional.of(participant1);
        }
        if (participant2 != null && !participant2.getId().equals(winner.getId())) {
            return Optional.of(participant2);
        }
        return Optional.empty();
    }

    private BigDecimal calculateTotalPrizePool(Tournament tournament) {
        int confirmedCount = participantRepository
                .findByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.CONFIRMED)
                .size();
        return tournament.getEntryFee().multiply(BigDecimal.valueOf(confirmedCount));
    }

    private Map<Integer, Integer> parsePrizeStructure(String prizeStructureJson) {
        if (prizeStructureJson == null || prizeStructureJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Integer> raw = objectMapper.readValue(prizeStructureJson, new TypeReference<Map<String, Integer>>() {
            });
            Map<Integer, Integer> result = new LinkedHashMap<>();
            raw.forEach((key, value) -> parseRank(key).ifPresent(rank -> result.put(rank, value)));
            return result;
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    // "1st", "2nd", "3rd" 형태의 키에서 순위 숫자만 추출한다.
    private Optional<Integer> parseRank(String key) {
        String digits = key.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(digits));
    }
}
