package com.esports.platform.domain.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.bracket.entity.Match;
import com.esports.platform.domain.bracket.repository.MatchRepository;
import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.settlement.entity.Settlement;
import com.esports.platform.domain.settlement.entity.SettlementStatus;
import com.esports.platform.domain.settlement.repository.SettlementRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.esports.platform.domain.tournament.repository.TournamentRepository;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentParticipantRepository participantRepository;

    @Mock
    private MatchRepository matchRepository;

    private SettlementService settlementService;

    private long participantSeq = 1;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                settlementRepository, tournamentRepository, participantRepository, matchRepository, new ObjectMapper()
        );
    }

    @Test
    void 미정산대회조회_이미정산된대회는_제외한다() {
        Tournament settled = createTournament(1L, 10L, BigDecimal.TEN, "{}");
        Tournament unsettled = createTournament(2L, 10L, BigDecimal.TEN, "{}");
        when(tournamentRepository.findByStatus(TournamentStatus.FINISHED)).thenReturn(List.of(settled, unsettled));
        when(settlementRepository.existsByTournamentId(1L)).thenReturn(true);
        when(settlementRepository.existsByTournamentId(2L)).thenReturn(false);

        List<Tournament> result = settlementService.findUnsettledFinishedTournaments();

        assertThat(result).containsExactly(unsettled);
    }

    @Test
    void 정산계산_경기없으면_빈리스트반환() {
        Tournament tournament = createTournament(1L, 10L, BigDecimal.TEN, "{\"1st\":100}");
        when(matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(1L)).thenReturn(List.of());

        List<Settlement> result = settlementService.calculateSettlements(tournament);

        assertThat(result).isEmpty();
    }

    @Test
    void 정산계산_결승전미종료시_빈리스트반환() {
        Tournament tournament = createTournament(1L, 10L, BigDecimal.TEN, "{\"1st\":100}");
        TournamentParticipant p1 = createParticipant(tournament, createUser(100L));
        TournamentParticipant p2 = createParticipant(tournament, createUser(101L));
        Match scheduledMatch = Match.createScheduled(tournament, 1, 1, p1, p2);
        when(matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(1L)).thenReturn(List.of(scheduledMatch));

        List<Settlement> result = settlementService.calculateSettlements(tournament);

        assertThat(result).isEmpty();
    }

    @Test
    void 정산계산_정상케이스_수수료차감후순위별정산생성() {
        Tournament tournament = createTournament(1L, 10L, new BigDecimal("10.33"), "{\"1st\":50,\"2nd\":30}");
        User winnerUser = createUser(100L);
        User loserUser = createUser(101L);
        TournamentParticipant winner = createParticipant(tournament, winnerUser);
        TournamentParticipant loser = createParticipant(tournament, loserUser);
        Match finalMatch = createFinishedMatch(tournament, 1, 1, winner, loser, winner);
        when(matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(1L)).thenReturn(List.of(finalMatch));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.CONFIRMED))
                .thenReturn(List.of(winner, loser, createParticipant(tournament, createUser(102L))));

        List<Settlement> result = settlementService.calculateSettlements(tournament);

        assertThat(result).hasSize(2);
        Settlement first = result.stream().filter(s -> s.getRank() == 1).findFirst().orElseThrow();
        Settlement second = result.stream().filter(s -> s.getRank() == 2).findFirst().orElseThrow();

        // totalPrizePool = 10.33 * 3 = 30.99
        // 1st: gross = 30.99*50/100 = 15.495 -> DOWN 15.49, fee = 1.549 -> DOWN 1.54, net = 13.95
        assertThat(first.getUser()).isEqualTo(winnerUser);
        assertThat(first.getPlatformFee()).isEqualByComparingTo("1.54");
        assertThat(first.getPrizeAmount()).isEqualByComparingTo("13.95");
        assertThat(first.isPending()).isTrue();

        // 2nd: gross = 30.99*30/100 = 9.297 -> DOWN 9.29, fee = 0.929 -> DOWN 0.92, net = 8.37
        assertThat(second.getUser()).isEqualTo(loserUser);
        assertThat(second.getPlatformFee()).isEqualByComparingTo("0.92");
        assertThat(second.getPrizeAmount()).isEqualByComparingTo("8.37");
    }

    @Test
    void 정산계산_상금구조에없는순위는_정산제외() {
        Tournament tournament = createTournament(1L, 10L, new BigDecimal("10.33"), "{\"1st\":50}");
        TournamentParticipant winner = createParticipant(tournament, createUser(100L));
        TournamentParticipant loser = createParticipant(tournament, createUser(101L));
        Match finalMatch = createFinishedMatch(tournament, 1, 1, winner, loser, winner);
        when(matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(1L)).thenReturn(List.of(finalMatch));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.CONFIRMED))
                .thenReturn(List.of(winner, loser));

        List<Settlement> result = settlementService.calculateSettlements(tournament);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRank()).isEqualTo(1);
    }

    @Test
    void 정산계산_상금구조파싱실패시_빈리스트반환() {
        Tournament tournament = createTournament(1L, 10L, new BigDecimal("10.33"), "이건 JSON이 아님");
        TournamentParticipant winner = createParticipant(tournament, createUser(100L));
        TournamentParticipant loser = createParticipant(tournament, createUser(101L));
        Match finalMatch = createFinishedMatch(tournament, 1, 1, winner, loser, winner);
        when(matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(1L)).thenReturn(List.of(finalMatch));
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.CONFIRMED))
                .thenReturn(List.of(winner, loser));

        List<Settlement> result = settlementService.calculateSettlements(tournament);

        assertThat(result).isEmpty();
    }

    @Test
    void 전체저장_저장소에위임() {
        Tournament tournament = createTournament(1L, 10L, BigDecimal.TEN, "{}");
        List<Settlement> settlements = List.of(Settlement.create(tournament, createUser(100L), 1, BigDecimal.TEN, BigDecimal.ONE));
        when(settlementRepository.saveAll(settlements)).thenReturn(settlements);

        List<Settlement> result = settlementService.saveAll(settlements);

        assertThat(result).isEqualTo(settlements);
    }

    @Test
    void 대기중정산일괄완료_상태전환() {
        Tournament tournament = createTournament(1L, 10L, BigDecimal.TEN, "{}");
        Settlement pending = Settlement.create(tournament, createUser(100L), 1, BigDecimal.TEN, BigDecimal.ONE);
        when(settlementRepository.findByStatus(SettlementStatus.PENDING)).thenReturn(List.of(pending));

        settlementService.completeAllPending();

        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(pending.getSettledAt()).isNotNull();
    }

    @Test
    void 내정산조회_저장소결과그대로반환() {
        Tournament tournament = createTournament(1L, 10L, BigDecimal.TEN, "{}");
        List<Settlement> settlements = List.of(Settlement.create(tournament, createUser(100L), 1, BigDecimal.TEN, BigDecimal.ONE));
        when(settlementRepository.findByUserId(100L)).thenReturn(settlements);

        List<Settlement> result = settlementService.findByUserId(100L);

        assertThat(result).isEqualTo(settlements);
    }

    @Test
    void 대회정산조회_정상요청시_목록반환() {
        Tournament tournament = createTournament(1L, 10L, BigDecimal.TEN, "{}");
        List<Settlement> settlements = List.of(Settlement.create(tournament, createUser(100L), 1, BigDecimal.TEN, BigDecimal.ONE));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(settlementRepository.findByTournamentId(1L)).thenReturn(settlements);

        List<Settlement> result = settlementService.findByTournamentId(1L, 10L);

        assertThat(result).isEqualTo(settlements);
    }

    @Test
    void 대회정산조회_대회없으면_예외발생() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.findByTournamentId(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_NOT_FOUND);
    }

    @Test
    void 대회정산조회_호스트아니면_예외발생() {
        Tournament tournament = createTournament(1L, 10L, BigDecimal.TEN, "{}");
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> settlementService.findByTournamentId(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_NOT_HOST);

        verify(settlementRepository, never()).findByTournamentId(any());
    }

    private User createUser(Long id) {
        User user = User.createLocalUser("user" + id + "@example.com", "encoded-password", "참가자" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Tournament createTournament(Long id, Long hostId, BigDecimal entryFee, String prizeStructure) {
        Tournament tournament = Tournament.create(
                createUser(hostId), "대회", "게임", TournamentFormat.SINGLE_ELIMINATION, 16,
                entryFee, prizeStructure, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        ReflectionTestUtils.setField(tournament, "id", id);
        return tournament;
    }

    private TournamentParticipant createParticipant(Tournament tournament, User user) {
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        ReflectionTestUtils.setField(participant, "id", participantSeq++);
        participant.confirm();
        return participant;
    }

    private Match createFinishedMatch(
            Tournament tournament, int round, int matchNumber,
            TournamentParticipant p1, TournamentParticipant p2, TournamentParticipant winner
    ) {
        Match match = Match.createScheduled(tournament, round, matchNumber, p1, p2);
        match.recordResult(winner);
        return match;
    }
}
