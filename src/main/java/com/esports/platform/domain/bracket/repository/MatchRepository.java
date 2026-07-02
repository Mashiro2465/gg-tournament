package com.esports.platform.domain.bracket.repository;

import com.esports.platform.domain.bracket.entity.Match;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByTournamentIdOrderByRoundAscMatchNumberAsc(Long tournamentId);

    List<Match> findByTournamentIdAndRound(Long tournamentId, int round);

    Optional<Match> findByTournamentIdAndRoundAndMatchNumber(Long tournamentId, int round, int matchNumber);

    boolean existsByTournamentId(Long tournamentId);
}
