package com.esports.platform.domain.participant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.repository.TournamentRepository;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.service.UserService;
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
class ParticipantJoinExecutorTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentParticipantRepository participantRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ParticipantJoinExecutor participantJoinExecutor;

    @Test
    void 무료대회_참가신청은_즉시확정된다() {
        Tournament tournament = createTournament(BigDecimal.ZERO);
        User participantUser = createUser(2L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(participantRepository.existsByTournamentIdAndUserIdAndStatusNot(any(), any(), any()))
                .thenReturn(false);
        when(userService.findById(2L)).thenReturn(participantUser);
        when(participantRepository.save(any(TournamentParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TournamentParticipant participant = participantJoinExecutor.execute(1L, 2L);

        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.CONFIRMED);
        assertThat(tournament.getCurrentParticipants()).isEqualTo(1);
    }

    @Test
    void 유료대회_참가신청은_결제대기상태다() {
        Tournament tournament = createTournament(BigDecimal.valueOf(10_000));
        User participantUser = createUser(2L);
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(participantRepository.existsByTournamentIdAndUserIdAndStatusNot(any(), any(), any()))
                .thenReturn(false);
        when(userService.findById(2L)).thenReturn(participantUser);
        when(participantRepository.save(any(TournamentParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TournamentParticipant participant = participantJoinExecutor.execute(1L, 2L);

        assertThat(participant.getStatus()).isEqualTo(ParticipantStatus.PENDING);
        assertThat(tournament.getCurrentParticipants()).isEqualTo(1);
    }

    private Tournament createTournament(BigDecimal entryFee) {
        Tournament tournament = Tournament.create(
                createUser(1L), "대회", "게임", TournamentFormat.SINGLE_ELIMINATION, 16,
                entryFee, "{}", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)
        );
        ReflectionTestUtils.setField(tournament, "id", 1L);
        return tournament;
    }

    private User createUser(Long id) {
        User user = User.createLocalUser("user" + id + "@example.com", "encoded-password", "사용자" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
