package com.esports.platform.domain.settlement.batch;

import com.esports.platform.domain.settlement.entity.Settlement;
import com.esports.platform.domain.settlement.service.SettlementService;
import com.esports.platform.domain.tournament.entity.Tournament;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementItemProcessor implements ItemProcessor<Tournament, List<Settlement>> {

    private final SettlementService settlementService;

    @Override
    public List<Settlement> process(Tournament tournament) {
        return settlementService.calculateSettlements(tournament);
    }
}
