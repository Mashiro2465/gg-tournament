package com.esports.platform.domain.tournament.repository;

import com.esports.platform.domain.tournament.entity.TournamentFormat;
import com.esports.platform.domain.tournament.entity.TournamentStatus;

public record TournamentSearchCondition(
        String keyword,
        String gameType,
        TournamentFormat format,
        TournamentStatus status
) {
}
