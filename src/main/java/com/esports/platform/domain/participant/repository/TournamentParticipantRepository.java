package com.esports.platform.domain.participant.repository;

import com.esports.platform.domain.participant.entity.ParticipantStatus;
import com.esports.platform.domain.participant.entity.TournamentParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {

    List<TournamentParticipant> findByTournamentId(Long tournamentId);

    Optional<TournamentParticipant> findByTournamentIdAndUserId(Long tournamentId, Long userId);

    boolean existsByTournamentIdAndUserIdAndStatusNot(Long tournamentId, Long userId, ParticipantStatus status);

    List<TournamentParticipant> findByUserId(Long userId);
}
