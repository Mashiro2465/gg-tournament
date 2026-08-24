package com.esports.platform.domain.participant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TournamentParticipantServiceTest {

    @Mock
    private TournamentParticipantRepository participantRepository;

    @Mock
    private ParticipantJoinExecutor participantJoinExecutor;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private TournamentParticipantService tournamentParticipantService;

    @Test
    void 참가신청_락획득성공시_참가처리위임() throws InterruptedException {
        when(redissonClient.getLock("lock:tournament:1")).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        TournamentParticipant participant = mock(TournamentParticipant.class);
        when(participantJoinExecutor.execute(1L, 100L)).thenReturn(participant);

        TournamentParticipant result = tournamentParticipantService.join(1L, 100L);

        assertThat(result).isEqualTo(participant);
        verify(rLock).unlock();
    }

    @Test
    void 참가신청_락획득실패시_예외발생() throws InterruptedException {
        when(redissonClient.getLock("lock:tournament:1")).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> tournamentParticipantService.join(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_LOCK_FAILED);

        verify(participantJoinExecutor, never()).execute(any(), any());
        verify(rLock, never()).unlock();
    }

    @Test
    void 참가취소_정상요청시_상태변경및정원감소() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L);
        ReflectionTestUtils.setField(tournament, "currentParticipants", 1);
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.of(participant));

        tournamentParticipantService.cancel(1L, 100L);

        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.CANCELLED);
        assertThat(tournament.getCurrentParticipants()).isEqualTo(0);
    }

    @Test
    void 참가취소_참가정보없으면_예외발생() {
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tournamentParticipantService.cancel(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    @Test
    void 참가취소_이미취소된참가면_예외발생() {
        User user = createUser(100L);
        Tournament tournament = createTournament(1L);
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        participant.cancel();
        when(participantRepository.findByTournamentIdAndUserId(1L, 100L)).thenReturn(Optional.of(participant));

        assertThatThrownBy(() -> tournamentParticipantService.cancel(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PARTICIPANT_NOT_FOUND);
    }

    private User createUser(Long id) {
        User user = User.createLocalUser("user@example.com", "encoded-password", "참가자");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Tournament createTournament(Long id) {
        Tournament tournament = Tournament.create(
                createUser(1L), "대회", "게임", TournamentFormat.SINGLE_ELIMINATION, 16,
                BigDecimal.ZERO, "{}", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3)
        );
        ReflectionTestUtils.setField(tournament, "id", id);
        return tournament;
    }
}
