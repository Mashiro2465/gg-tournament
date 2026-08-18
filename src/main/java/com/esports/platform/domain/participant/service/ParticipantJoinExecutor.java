package com.esports.platform.domain.participant.service;

import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.esports.platform.domain.tournament.repository.TournamentRepository;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.service.UserService;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis 분산 락을 보유한 TournamentParticipantService가 호출하는 참가 신청 트랜잭션.
 * 같은 클래스 내 self-invocation은 Spring AOP 프록시를 거치지 않아 @Transactional이
 * 적용되지 않으므로, 별도 빈으로 분리해 락 보유 중 독립된 트랜잭션이 커밋되도록 한다.
 */
@Component
@RequiredArgsConstructor
class ParticipantJoinExecutor {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final UserService userService;

    @Transactional
    public TournamentParticipant execute(Long tournamentId, Long userId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOURNAMENT_NOT_FOUND));

        if (tournament.getStatus() != TournamentStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.TOURNAMENT_ALREADY_CLOSED);
        }

        if (participantRepository.existsByTournamentIdAndUserIdAndStatusNot(
                tournamentId, userId, ParticipantStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.PARTICIPANT_ALREADY_JOINED);
        }

        tournament.increaseParticipantCount();

        User user = userService.findById(userId);
        TournamentParticipant participant = TournamentParticipant.create(tournament, user);
        if (tournament.getEntryFee().compareTo(BigDecimal.ZERO) == 0) {
            participant.confirm();
        }
        return participantRepository.save(participant);
    }
}
