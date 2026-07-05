package com.esports.platform.domain.settlement.batch;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    // 매일 자정 실행. 상금 정산은 실시간 API 대신 배치로 처리해 트랜잭션 안정성과
    // 재시작 가능성(장애 시 완료되지 않은 단계부터 재처리)을 확보한다.
    @Scheduled(cron = "0 0 0 * * *")
    public void runSettlementJob() {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate("runDate", LocalDate.now())
                .toJobParameters();
        try {
            jobLauncher.run(settlementJob, jobParameters);
        } catch (Exception e) {
            log.error("정산 배치 실행 실패", e);
        }
    }
}
