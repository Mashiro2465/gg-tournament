package com.esports.platform.domain.tournament.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.esports.platform.domain.tournament.repository.TournamentRepository;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.service.UserService;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void 대회생성_정상요청시_저장() {
        User host = createUser(1L);
        when(userService.findById(1L)).thenReturn(host);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tournament result = tournamentService.create(
                1L, "롤 초보자 대회", "리그오브레전드", TournamentFormat.SINGLE_ELIMINATION,
                16, BigDecimal.valueOf(10000), "{\"1st\":50,\"2nd\":30,\"3rd\":20}",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );

        assertThat(result.getTitle()).isEqualTo("롤 초보자 대회");
        assertThat(result.getHost()).isEqualTo(host);
        assertThat(result.getStatus()).isEqualTo(TournamentStatus.RECRUITING);
    }

    @Test
    void 대회조회_존재하지않는id면_예외발생() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tournamentService.findById(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_NOT_FOUND);
    }

    @Test
    void 대회수정_주최자아니면_예외발생() {
        Tournament tournament = createTournament(1L, 100L, TournamentStatus.RECRUITING);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> tournamentService.update(
                1L, 999L, "제목", "게임", 16, "{}", LocalDateTime.now(), LocalDateTime.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_NOT_HOST);
    }

    @Test
    void 대회수정_모집중아니면_예외발생() {
        Tournament tournament = createTournament(1L, 100L, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> tournamentService.update(
                1L, 100L, "제목", "게임", 16, "{}", LocalDateTime.now(), LocalDateTime.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_ALREADY_CLOSED);
    }

    @Test
    void 대회취소_정상요청시_상태변경() {
        Tournament tournament = createTournament(1L, 100L, TournamentStatus.RECRUITING);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        tournamentService.cancel(1L, 100L);

        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.CANCELLED);
    }

    @Test
    void 대회취소_주최자아니면_예외발생() {
        Tournament tournament = createTournament(1L, 100L, TournamentStatus.RECRUITING);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> tournamentService.cancel(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_NOT_HOST);
    }

    @Test
    void 대회시작_정상요청시_상태변경() {
        Tournament tournament = createTournament(1L, 100L, TournamentStatus.RECRUITING);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        tournamentService.start(1L, 100L);

        assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    }

    @Test
    void 대회시작_이미시작된대회면_예외발생() {
        Tournament tournament = createTournament(1L, 100L, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> tournamentService.start(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOURNAMENT_ALREADY_CLOSED);
    }

    private User createUser(Long id) {
        User user = User.createLocalUser("host@example.com", "encoded-password", "주최자");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Tournament createTournament(Long tournamentId, Long hostId, TournamentStatus status) {
        Tournament tournament = Tournament.create(
                createUser(hostId), "대회", "게임", TournamentFormat.SINGLE_ELIMINATION, 16,
                BigDecimal.ZERO, "{}", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        ReflectionTestUtils.setField(tournament, "id", tournamentId);
        ReflectionTestUtils.setField(tournament, "status", status);
        return tournament;
    }
}
