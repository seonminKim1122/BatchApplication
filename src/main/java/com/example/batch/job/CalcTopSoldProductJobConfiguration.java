package com.example.batch.job;

import com.example.batch.entity.PopularProduct;
import com.example.batch.entity.Product;
import com.example.batch.mapper.ProductIdRowMapper;
import com.example.batch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final ProductRepository productRepository;

    private final int chunkSize = 20;
    private final String prefix = "popular::";
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Bean
    public Job calcPopularProductJob() throws SQLException {
        return jobBuilderFactory.get("calcTopSoldProductJob")
                .start(calcPopularProductStep())
                .build();
    }

    @Bean
    public Step calcPopularProductStep() throws SQLException {
        return stepBuilderFactory.get("calcPopularProductStep")
                .<Product, Product>chunk(chunkSize)
                .reader(popularProductReader())
                .processor(popularProductProcessor())
                .writer(popularProductWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Product> popularProductReader() {
        String yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(2L).format(FORMATTER);

        return new JdbcCursorItemReaderBuilder<Product>()
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new ProductIdRowMapper())
                .sql("SELECT product_product_id " +
                     "FROM purchase " +
                     "WHERE purchase_date = '" + yesterday + "' " +
                     "GROUP BY product_product_id " +
                     "ORDER BY sum(amount) DESC " +
                     "LIMIT " + chunkSize)
                .name("popularProductReader")
                .build();
    }

    @Bean
    public ItemProcessor<Product, Product> popularProductProcessor() {
        return item -> productRepository.findById(item.getId()).orElseThrow();
    }
    private ItemWriter<Product> popularProductWriter() { // Redis 에 저장하기
        return list -> {
            List<PopularProduct> popularProducts = list.stream().map(PopularProduct::new).toList();
            String key = prefix + new SimpleDateFormat("yy. M. d.").format(new Date());
            System.out.println(key);
            redisTemplate.delete(key);
            redisTemplate.opsForValue().setIfAbsent(key, popularProducts);
            redisTemplate.expire(key, Duration.ofHours(24));
        };
    }
}
