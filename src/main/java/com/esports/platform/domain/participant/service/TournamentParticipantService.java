package com.esports.platform.domain.participant.service;

import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.participant.repository.TournamentParticipantRepository;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TournamentParticipantService {

    private static final String LOCK_KEY_PREFIX = "lock:tournament:";
    private static final long LOCK_WAIT_SECONDS = 3L;
    private static final long LOCK_LEASE_SECONDS = 3L;

    private final TournamentParticipantRepository participantRepository;
    private final ParticipantJoinExecutor participantJoinExecutor;
    private final RedissonClient redissonClient;

    // join()에는 @Transactional을 붙이지 않는다. 붙이면 커밋이 unlock()보다 늦게 일어나
    // 락 해제 직후 다른 스레드가 아직 반영되지 않은 정원 수를 읽는 경합이 생길 수 있다.
    public TournamentParticipant join(Long tournamentId, Long userId) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + tournamentId);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException(ErrorCode.PARTICIPANT_LOCK_FAILED);
            }
            return participantJoinExecutor.execute(tournamentId, userId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PARTICIPANT_LOCK_FAILED);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    public void cancel(Long tournamentId, Long userId) {
        TournamentParticipant participant = findParticipantOrThrow(tournamentId, userId);
        if (participant.isCancelled()) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        participant.cancel();
        participant.getTournament().decreaseParticipantCount();
    }

    @Transactional(readOnly = true)
    public List<TournamentParticipant> findByTournamentId(Long tournamentId) {
        return participantRepository.findByTournamentId(tournamentId);
    }

    private TournamentParticipant findParticipantOrThrow(Long tournamentId, Long userId) {
        return participantRepository.findByTournamentIdAndUserId(tournamentId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }
}
