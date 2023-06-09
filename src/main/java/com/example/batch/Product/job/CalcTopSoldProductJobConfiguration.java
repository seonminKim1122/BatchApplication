package com.example.batch.Product.job;

import com.example.batch.Product.entity.Product;
import com.example.batch.Product.mapper.ProductRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CalcTopSoldProductJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final RedisTemplate<String, List<?>> redisTemplate;

    private final int chunkSize = 20;
    private final String prefix = "product::";

    @Bean
    public Job calcTopSoldProductJob() {
        return jobBuilderFactory.get("calcTopSoldProductJob")
                .start(calcTopSoldProductStep())
                .build();
    }

    @Bean
    public Step calcTopSoldProductStep() {
        return stepBuilderFactory.get("calcTopSoldProductStep")
                .<Product, Product>chunk(chunkSize)
                .reader(topSoldProductReader())
                .writer(topSoldProductWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Product> topSoldProductReader() {
        return new JdbcCursorItemReaderBuilder<Product>()
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new ProductRowMapper())
                .sql("SELECT p.* " +
                     "FROM products p LEFT JOIN purchase ph ON p.product_id = ph.product_product_id " +
                     "WHERE DATE_FORMAT(ph.create_at, '%Y-%m-%d') = CURDATE() - INTERVAL 1 DAY " +
                     "GROUP BY p.product_id " +
                     "ORDER BY sum(ph.amount) DESC " +
                     "LIMIT " + chunkSize)
                .name("topSoldProductReader")
                .build();
    }

    private ItemWriter<Product> topSoldProductWriter() { // Redis 에 저장하기
        return list -> {
            String key = prefix + new SimpleDateFormat("yyyyMMdd").format(new Date());
            redisTemplate.opsForValue().setIfAbsent(key, list);
            redisTemplate.expire(key, Duration.ofHours(24));
        };
    }


}
