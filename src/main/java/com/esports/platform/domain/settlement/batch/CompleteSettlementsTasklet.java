package com.esports.platform.domain.settlement.batch;

import com.esports.platform.domain.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompleteSettlementsTasklet implements Tasklet {

    private final SettlementService settlementService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        settlementService.completeAllPending();
        return RepeatStatus.FINISHED;
    }
}
