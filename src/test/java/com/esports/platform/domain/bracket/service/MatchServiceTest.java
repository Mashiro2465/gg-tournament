package com.esports.platform.domain.bracket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.bracket.entity.Match;
import com.esports.platform.domain.bracket.entity.MatchStatus;
import com.esports.platform.domain.bracket.repository.MatchRepository;
import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.esports.platform.domain.tournament.service.TournamentService;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentParticipantRepository participantRepository;

    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private MatchService matchService;

    private long participantSeq = 1;

    @Test
    void 대진표생성_이미생성되어있으면_예외발생() {
        when(matchRepository.existsByTournamentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> matchService.generateBracket(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BRACKET_ALREADY_GENERATED);

        verify(tournamentService, never()).start(any(), any());
    }

    @Test
    void 대진표생성_참가자2명미만이면_예외발생() {
        when(matchRepository.existsByTournamentId(1L)).thenReturn(false);
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.CONFIRMED))
                .thenReturn(List.of(createParticipant(createTournament(1L, 10L), createUser(100L))));

        assertThatThrownBy(() -> matchService.generateBracket(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BRACKET_NOT_ENOUGH_PARTICIPANTS);

        verify(tournamentService, never()).start(any(), any());
    }

    @Test
    void 대진표생성_참가자가2의거듭제곱이면_부전승없이1라운드생성() {
        Tournament tournament = createTournament(1L, 10L);
        List<TournamentParticipant> participants = List.of(
                createParticipant(tournament, createUser(100L)),
                createParticipant(tournament, createUser(101L)),
                createParticipant(tournament, createUser(102L)),
                createParticipant(tournament, createUser(103L))
        );
        when(matchRepository.existsByTournamentId(1L)).thenReturn(false);
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.CONFIRMED)).thenReturn(participants);
        when(tournamentService.findById(1L)).thenReturn(tournament);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Match> result = matchService.generateBracket(1L, 10L);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(match -> match.getRound() == 1 && match.getStatus() == MatchStatus.SCHEDULED);
        assertThat(result).extracting(Match::getMatchNumber).containsExactlyInAnyOrder(1, 2);

        List<TournamentParticipant> assigned = result.stream()
                .flatMap(match -> Stream.of(match.getParticipant1(), match.getParticipant2()))
                .toList();
        assertThat(assigned).containsExactlyInAnyOrderElementsOf(participants);

        verify(tournamentService).start(1L, 10L);
        verify(matchRepository, times(2)).save(any(Match.class));
        verify(matchRepository, never()).findByTournamentIdAndRound(any(), anyInt());
    }

    @Test
    void 대진표생성_참가자가2의거듭제곱아니면_부전승발생하고즉시다음라운드진출() {
        Tournament tournament = createTournament(1L, 10L);
        List<TournamentParticipant> participants = List.of(
                createParticipant(tournament, createUser(100L)),
                createParticipant(tournament, createUser(101L)),
                createParticipant(tournament, createUser(102L))
        );
        when(matchRepository.existsByTournamentId(1L)).thenReturn(false);
        when(participantRepository.findByTournamentIdAndStatus(1L, ParticipantStatus.CONFIRMED)).thenReturn(participants);
        when(tournamentService.findById(1L)).thenReturn(tournament);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 부전승 진출 처리 시 현재 라운드가 결승이 아님을 알리기 위해 노드 2개(부전승 1 + 일반경기 1)를 반환
        when(matchRepository.findByTournamentIdAndRound(1L, 1))
                .thenReturn(List.of(mock(Match.class), mock(Match.class)));
        when(matchRepository.findByTournamentIdAndRoundAndMatchNumber(1L, 2, 1)).thenReturn(Optional.empty());

        List<Match> result = matchService.generateBracket(1L, 10L);

        assertThat(result).hasSize(2);
        Match byeMatch = result.stream().filter(match -> match.getMatchNumber() == 1).findFirst().orElseThrow();
        Match normalMatch = result.stream().filter(match -> match.getMatchNumber() == 2).findFirst().orElseThrow();
        assertThat(byeMatch.isBye()).isTrue();
        assertThat(byeMatch.getWinner()).isEqualTo(byeMatch.getParticipant1());
        assertThat(normalMatch.getStatus()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(normalMatch.getParticipant1()).isNotNull();
        assertThat(normalMatch.getParticipant2()).isNotNull();

        ArgumentCaptor<Match> savedCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository, times(3)).save(savedCaptor.capture());
        Match advancedMatch = savedCaptor.getAllValues().get(2);
        assertThat(advancedMatch.getRound()).isEqualTo(2);
        assertThat(advancedMatch.getMatchNumber()).isEqualTo(1);
        assertThat(advancedMatch.getParticipant1()).isEqualTo(byeMatch.getWinner());
        assertThat(advancedMatch.getParticipant2()).isNull();
    }

    @Test
    void 결과입력_정상요청시_승자기록및다음라운드진출() {
        Tournament tournament = createTournament(1L, 10L);
        TournamentParticipant p1 = createParticipant(tournament, createUser(100L));
        TournamentParticipant p2 = createParticipant(tournament, createUser(101L));
        Match match = createMatch(tournament, 1, 1, p1, p2);
        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));
        when(participantRepository.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(matchRepository.findByTournamentIdAndRound(1L, 1))
                .thenReturn(List.of(mock(Match.class), mock(Match.class)));
        when(matchRepository.findByTournamentIdAndRoundAndMatchNumber(1L, 2, 1)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Match result = matchService.recordResult(5L, 10L, p1.getId());

        assertThat(result.isFinished()).isTrue();
        assertThat(result.getWinner()).isEqualTo(p1);

        ArgumentCaptor<Match> savedCaptor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getRound()).isEqualTo(2);
        assertThat(savedCaptor.getValue().getParticipant1()).isEqualTo(p1);
    }

    @Test
    void 결과입력_경기없으면_예외발생() {
        when(matchRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.recordResult(5L, 10L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MATCH_NOT_FOUND);
    }

    @Test
    void 결과입력_호스트아니면_예외발생() {
        Tournament tournament = createTournament(1L, 10L);
        TournamentParticipant p1 = createParticipant(tournament, createUser(100L));
        TournamentParticipant p2 = createParticipant(tournament, createUser(101L));
        Match match = createMatch(tournament, 1, 1, p1, p2);
        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> matchService.recordResult(5L, 999L, p1.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_NOT_HOST);

        verify(participantRepository, never()).findById(any());
    }

    @Test
    void 결과입력_존재하지않는참가자면_예외발생() {
        Tournament tournament = createTournament(1L, 10L);
        TournamentParticipant p1 = createParticipant(tournament, createUser(100L));
        TournamentParticipant p2 = createParticipant(tournament, createUser(101L));
        Match match = createMatch(tournament, 1, 1, p1, p2);
        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));
        when(participantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.recordResult(5L, 10L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 결과입력_해당경기참가자아니면_예외발생() {
        Tournament tournament = createTournament(1L, 10L);
        TournamentParticipant p1 = createParticipant(tournament, createUser(100L));
        TournamentParticipant p2 = createParticipant(tournament, createUser(101L));
        TournamentParticipant outsider = createParticipant(tournament, createUser(200L));
        Match match = createMatch(tournament, 1, 1, p1, p2);
        when(matchRepository.findById(5L)).thenReturn(Optional.of(match));
        when(participantRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> matchService.recordResult(5L, 10L, outsider.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MATCH_INVALID_WINNER);
    }

    @Test
    void 결과입력_결승전이면_대회종료처리() {
        Tournament tournament = createTournament(1L, 10L);
        TournamentParticipant p1 = createParticipant(tournament, createUser(100L));
        TournamentParticipant p2 = createParticipant(tournament, createUser(101L));
        Match finalMatch = createMatch(tournament, 2, 1, p1, p2);
        when(matchRepository.findById(5L)).thenReturn(Optional.of(finalMatch));
        when(participantRepository.findById(p1.getId())).thenReturn(Optional.of(p1));
        when(matchRepository.findByTournamentIdAndRound(1L, 2)).thenReturn(List.of(mock(Match.class)));

        matchService.recordResult(5L, 10L, p1.getId());

        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.FINISHED);
        verify(matchRepository, never()).findByTournamentIdAndRoundAndMatchNumber(any(), anyInt(), anyInt());
    }

    @Test
    void 대진표조회_저장소결과그대로반환() {
        Tournament tournament = createTournament(1L, 10L);
        TournamentParticipant p1 = createParticipant(tournament, createUser(100L));
        TournamentParticipant p2 = createParticipant(tournament, createUser(101L));
        List<Match> matches = List.of(createMatch(tournament, 1, 1, p1, p2));
        when(matchRepository.findByTournamentIdOrderByRoundAscMatchNumberAsc(1L)).thenReturn(matches);

        List<Match> result = matchService.findBracket(1L);

        assertThat(result).isEqualTo(matches);
    }

    private User createUser(Long id) {
        User user = User.createLocalUser("user" + id + "@example.com", "encoded-password", "참가자" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Tournament createTournament(Long id, Long hostId) {
        Tournament tournament = Tournament.create(
                createUser(hostId), "대회", "게임", TournamentFormat.SINGLE_ELIMINATION, 16,
                BigDecimal.ZERO, "{}", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
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

    private Match createMatch(Tournament tournament, int round, int matchNumber, TournamentParticipant p1, TournamentParticipant p2) {
        Match match = Match.createScheduled(tournament, round, matchNumber, p1, p2);
        ReflectionTestUtils.setField(match, "id", 5L);
        return match;
    }
}
