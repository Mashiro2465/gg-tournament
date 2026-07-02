package com.esports.platform.domain.bracket.dto;

import com.esports.platform.domain.bracket.entity.Match;
import com.esports.platform.domain.bracket.entity.MatchStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        Long tournamentId,
        int round,
        int matchNumber,
        Long participant1Id,
        String participant1Nickname,
        Long participant2Id,
        String participant2Nickname,
        Long winnerId,
        String winnerNickname,
        MatchStatus status,
        LocalDateTime playedAt
) {

    public static MatchResponse from(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getTournament().getId(),
                match.getRound(),
                match.getMatchNumber(),
                participantId(match.getParticipant1()),
                participantNickname(match.getParticipant1()),
                participantId(match.getParticipant2()),
                participantNickname(match.getParticipant2()),
                participantId(match.getWinner()),
                participantNickname(match.getWinner()),
                match.getStatus(),
                match.getPlayedAt()
        );
    }

    private static Long participantId(TournamentParticipant participant) {
        return participant != null ? participant.getId() : null;
    }

    private static String participantNickname(TournamentParticipant participant) {
        return participant != null ? participant.getUser().getNickname() : null;
    }
}
