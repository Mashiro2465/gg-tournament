package com.esports.platform.domain.tournament.dto;

import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TournamentSummaryResponse(
        Long id,
        String title,
        String gameType,
        TournamentFormat format,
        int maxParticipants,
        int currentParticipants,
        BigDecimal entryFee,
        BigDecimal prizePool,
        TournamentStatus status,
        LocalDateTime registrationDeadline,
        LocalDateTime startAt
) {

    public static TournamentSummaryResponse from(Tournament tournament) {
        return new TournamentSummaryResponse(
                tournament.getId(),
                tournament.getTitle(),
                tournament.getGameType(),
                tournament.getFormat(),
                tournament.getMaxParticipants(),
                tournament.getCurrentParticipants(),
                tournament.getEntryFee(),
                tournament.getPrizePool(),
                tournament.getStatus(),
                tournament.getRegistrationDeadline(),
                tournament.getStartAt()
        );
    }
}
