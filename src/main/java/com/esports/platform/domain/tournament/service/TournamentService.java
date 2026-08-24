package com.esports.platform.domain.tournament.service;

import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import com.esports.platform.domain.tournament.repository.TournamentRepository;
import com.esports.platform.domain.tournament.repository.TournamentSearchCondition;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.domain.user.service.UserService;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final UserService userService;

    @Transactional
    public Tournament create(
            Long hostId,
            String title,
            String gameType,
            TournamentFormat format,
            int maxParticipants,
            BigDecimal entryFee,
            String prizeStructure,
            LocalDateTime registrationDeadline,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        validateSchedule(registrationDeadline, startAt, endAt);
        User host = userService.findById(hostId);
        Tournament tournament = Tournament.create(
                host, title, gameType, format, maxParticipants, entryFee,
                prizeStructure, registrationDeadline, startAt, endAt
        );
        return tournamentRepository.save(tournament);
    }

    public Tournament findById(Long tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOURNAMENT_NOT_FOUND));
    }

    public Page<Tournament> search(TournamentSearchCondition condition, Pageable pageable) {
        return tournamentRepository.search(condition, pageable);
    }

    public List<Tournament> findByHostId(Long hostId) {
        return tournamentRepository.findByHostId(hostId);
    }

    @Transactional
    public void update(
            Long tournamentId,
            Long hostId,
            String title,
            String gameType,
            int maxParticipants,
            String prizeStructure,
            LocalDateTime registrationDeadline,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        Tournament tournament = findById(tournamentId);
        validateHost(tournament, hostId);
        validateRecruiting(tournament);
        validateSchedule(registrationDeadline, startAt, endAt);
        tournament.updateDetails(title, gameType, maxParticipants, prizeStructure, registrationDeadline, startAt, endAt);
    }

    @Transactional
    public void cancel(Long tournamentId, Long hostId) {
        Tournament tournament = findById(tournamentId);
        validateHost(tournament, hostId);
        validateRecruiting(tournament);
        tournament.cancel();
    }

    @Transactional
    public void start(Long tournamentId, Long hostId) {
        Tournament tournament = findById(tournamentId);
        validateHost(tournament, hostId);
        validateRecruiting(tournament);
        tournament.start();
    }

    private void validateHost(Tournament tournament, Long userId) {
        if (!tournament.isHost(userId)) {
            throw new BusinessException(ErrorCode.TOURNAMENT_NOT_HOST);
        }
    }

    private void validateRecruiting(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.RECRUITING) {
            throw new BusinessException(ErrorCode.TOURNAMENT_ALREADY_CLOSED);
        }
    }

    private void validateSchedule(
            LocalDateTime registrationDeadline,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (!registrationDeadline.isBefore(startAt) || !startAt.isBefore(endAt)) {
            throw new BusinessException(ErrorCode.INVALID_TOURNAMENT_SCHEDULE);
        }
    }
}
