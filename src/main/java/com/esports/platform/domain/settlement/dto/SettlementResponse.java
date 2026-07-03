package com.esports.platform.domain.settlement.dto;

import com.esports.platform.domain.settlement.entity.Settlement;
import com.esports.platform.domain.settlement.entity.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettlementResponse(
        Long id,
        Long tournamentId,
        String tournamentTitle,
        Long userId,
        String nickname,
        int rank,
        BigDecimal prizeAmount,
        BigDecimal platformFee,
        SettlementStatus status,
        LocalDateTime settledAt
) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getTournament().getId(),
                settlement.getTournament().getTitle(),
                settlement.getUser().getId(),
                settlement.getUser().getNickname(),
                settlement.getRank(),
                settlement.getPrizeAmount(),
                settlement.getPlatformFee(),
                settlement.getStatus(),
                settlement.getSettledAt()
        );
    }
}
