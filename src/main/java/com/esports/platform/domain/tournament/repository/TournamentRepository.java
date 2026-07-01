package com.esports.platform.domain.tournament.repository;

import com.esports.platform.domain.tournament.entity.Tournament;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long>, TournamentRepositoryCustom {

    List<Tournament> findByHostId(Long hostId);
}
