package com.example.batch.scheduler;

import com.example.batch.job.CalcTopSoldProductJobConfiguration;
import com.example.batch.job.CreatePurchaseDataJobConfiguration;
import com.example.batch.job.ProductManagementJobConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final ProductManagementJobConfiguration productManagementJobConfiguration;
    private final CalcTopSoldProductJobConfiguration calcTop90JobConfiguration;
    private final CreatePurchaseDataJobConfiguration createPurchaseDataJobConfiguration;

    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void runInventoryManagementJob() {
        // job parameter 설정
        JobParameters jobParameters = createJobParameters(LocalDate.now(ZoneId.of("Asia/Seoul")));
        try {
            jobLauncher.run(productManagementJobConfiguration.productManagementJob(), jobParameters);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runCalcTop90ProductJob() {
        JobParameters jobParameters = createJobParameters(LocalDate.now(ZoneId.of("Asia/Seoul")));

        try {
            jobLauncher.run(calcTop90JobConfiguration.calcTopSoldProductJob(), jobParameters);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 * * * *", zone = "Asia/Seoul")
    public void runCreatePurchaseDataJob() {
        Map<String, JobParameter> map = new HashMap<>();
        String now = LocalDateTime.now().toString();
        map.put("time", new JobParameter(now));
        JobParameters jobParameters = new JobParameters(map);

        try {
            jobLauncher.run(createPurchaseDataJobConfiguration.createPurchaseDataJob(), jobParameters);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public JobParameters createJobParameters (LocalDate date) {
        Map<String, JobParameter> map = new HashMap<>();
        String today = date.format(FORMATTER);
        map.put("date", new JobParameter(today));
        return new JobParameters(map);
    }
}
