package com.esports.platform.domain.settlement.batch;

import com.esports.platform.domain.settlement.entity.Settlement;
import com.esports.platform.domain.settlement.service.SettlementService;
import com.esports.platform.domain.tournament.entity.Tournament;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SettlementJobConfig {

    private static final int CHUNK_SIZE = 10;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SettlementService settlementService;
    private final SettlementItemProcessor settlementItemProcessor;
    private final SettlementItemWriter settlementItemWriter;
    private final CompleteSettlementsTasklet completeSettlementsTasklet;

    @Bean
    public Job settlementJob() {
        return new JobBuilder("settlementJob", jobRepository)
                .start(calculateSettlementsStep())
                .next(completeSettlementsStep())
                .build();
    }

    // 1단계: FINISHED 대회 중 아직 정산되지 않은 대회의 순위를 계산해 Settlement(PENDING)를 생성한다.
    @Bean
    public Step calculateSettlementsStep() {
        return new StepBuilder("calculateSettlementsStep", jobRepository)
                .<Tournament, List<Settlement>>chunk(CHUNK_SIZE, transactionManager)
                .reader(unsettledTournamentReader())
                .processor(settlementItemProcessor)
                .writer(settlementItemWriter)
                .build();
    }

    // 2단계: PENDING 상태 Settlement를 COMPLETED로 전환한다. 1단계 이후 장애가 발생해도
    // 이 단계만 재실행하면 되므로 배치 재시작 시 중복 정산 없이 이어서 처리할 수 있다.
    @Bean
    public Step completeSettlementsStep() {
        return new StepBuilder("completeSettlementsStep", jobRepository)
                .tasklet(completeSettlementsTasklet, transactionManager)
                .build();
    }

    // Job 실행 시점마다 최신 미정산 대회 목록을 읽어야 하므로 StepScope로 지연 생성한다.
    @Bean
    @StepScope
    public ListItemReader<Tournament> unsettledTournamentReader() {
        return new ListItemReader<>(settlementService.findUnsettledFinishedTournaments());
    }
}
