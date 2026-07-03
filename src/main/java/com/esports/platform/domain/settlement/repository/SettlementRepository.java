package com.esports.platform.domain.settlement.repository;

import com.esports.platform.domain.settlement.entity.Settlement;
import com.esports.platform.domain.settlement.entity.SettlementStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByUserId(Long userId);

    List<Settlement> findByTournamentId(Long tournamentId);

    List<Settlement> findByStatus(SettlementStatus status);

    boolean existsByTournamentId(Long tournamentId);
}
