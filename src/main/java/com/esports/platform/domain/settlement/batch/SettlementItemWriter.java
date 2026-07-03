package com.esports.platform.domain.settlement.batch;

import com.esports.platform.domain.settlement.entity.Settlement;
import com.esports.platform.domain.settlement.service.SettlementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementItemWriter implements ItemWriter<List<Settlement>> {

    private final SettlementService settlementService;

    @Override
    public void write(Chunk<? extends List<Settlement>> chunk) {
        List<Settlement> settlements = chunk.getItems().stream()
                .flatMap(List::stream)
                .toList();
        settlementService.saveAll(settlements);
    }
}
