package com.esports.platform.domain.tournament.dto;

import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TournamentDetailResponse(
        Long id,
        Long hostId,
        String hostNickname,
        String title,
        String gameType,
        TournamentFormat format,
        int maxParticipants,
        int currentParticipants,
        BigDecimal entryFee,
        BigDecimal prizePool,
        String prizeStructure,
        TournamentStatus status,
        LocalDateTime registrationDeadline,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime createdAt
) {

    public static TournamentDetailResponse from(Tournament tournament) {
        return new TournamentDetailResponse(
                tournament.getId(),
                tournament.getHost().getId(),
                tournament.getHost().getNickname(),
                tournament.getTitle(),
                tournament.getGameType(),
                tournament.getFormat(),
                tournament.getMaxParticipants(),
                tournament.getCurrentParticipants(),
                tournament.getEntryFee(),
                tournament.getPrizePool(),
                tournament.getPrizeStructure(),
                tournament.getStatus(),
                tournament.getRegistrationDeadline(),
                tournament.getStartAt(),
                tournament.getEndAt(),
                tournament.getCreatedAt()
        );
    }
}
