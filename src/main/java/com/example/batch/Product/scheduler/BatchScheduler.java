package com.example.batch.Product.scheduler;

import com.example.batch.Product.job.ProductManagementJobConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final ProductManagementJobConfiguration productManagementJobConfiguration;
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Scheduled(cron = "0 0 5 * * *", zone = "JST") // 도쿄와 서울이 시차가 없으므로 도쿄 기준 새벽 5시에 실행
    public void runJob() {
        // job parameter 설정
        Map<String, JobParameter> map = new HashMap<>();
        String today = LocalDate.now().format(FORMATTER);
        map.put("date", new JobParameter(today));
        JobParameters jobParameters = new JobParameters(map);
        try {
            jobLauncher.run(productManagementJobConfiguration.productManagementJob(), jobParameters);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
