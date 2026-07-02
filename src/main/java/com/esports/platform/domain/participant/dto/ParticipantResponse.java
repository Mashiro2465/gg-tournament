package com.esports.platform.domain.participant.dto;

import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import java.time.LocalDateTime;

public record ParticipantResponse(
        Long id,
        Long tournamentId,
        Long userId,
        String nickname,
        ParticipantStatus status,
        LocalDateTime joinedAt
) {

    public static ParticipantResponse from(TournamentParticipant participant) {
        return new ParticipantResponse(
                participant.getId(),
                participant.getTournament().getId(),
                participant.getUser().getId(),
                participant.getUser().getNickname(),
                participant.getStatus(),
                participant.getJoinedAt()
        );
    }
}
