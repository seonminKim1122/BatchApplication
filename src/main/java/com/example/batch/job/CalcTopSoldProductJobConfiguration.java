package com.example.batch.job;

import com.example.batch.entity.Product;
import com.example.batch.mapper.ProductIdRowMapper;
import com.example.batch.mapper.ProductRowMapper;
import com.example.batch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
    public Job calcTopSoldProductJob() throws SQLException {
        return jobBuilderFactory.get("calcTopSoldProductJob")
                .start(calcTopSoldProductStep())
                .next(writeToCacheStep())
                .build();
    }

    @Bean
    public Step calcTopSoldProductStep() throws SQLException {
        return stepBuilderFactory.get("calcTopSoldProductStep")
                .<Product, Product>chunk(chunkSize)
                .reader(topSoldProductIdReader())
                .processor(topSoldProductProcessor())
                .writer(topSoldProductWriter())
                .build();
    }

    @Bean
    public Step writeToCacheStep() {
        return stepBuilderFactory.get("writeToCacheStep")
                .<Product, Product>chunk(chunkSize)
                .reader(todayPopularProductReader())
                .writer(todayPopularProductWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Product> topSoldProductIdReader() {
        String yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1L).format(FORMATTER);
        return new JdbcCursorItemReaderBuilder<Product>()
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new ProductIdRowMapper())
                .sql("SELECT product_product_id " +
                     "FROM purchase " +
//                     "WHERE DATE_FORMAT(create_at, '%Y-%m-%d') = '" + yesterday + "' " +
                     "WHERE purchase_date = '" + yesterday + "' " +
                     "GROUP BY product_product_id " +
                     "ORDER BY sum(amount) DESC " +
                     "LIMIT " + chunkSize)
                .name("topSoldProductIdReader")
                .build();
    }

    @Bean
    public ItemProcessor<Product, Product> topSoldProductProcessor() {
        return item -> productRepository.findById(item.getId()).orElseThrow();
    }

    @Bean
    public JdbcBatchItemWriter<Product> topSoldProductWriter() throws SQLException {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement("delete from popular_products");
        statement.executeUpdate();
        connection.close();
        return new JdbcBatchItemWriterBuilder<Product>()
                .dataSource(dataSource)
                .sql("insert into popular_products (product_id, amount, categorya, categoryb, price, product_name) values (:id, :amount, :categoryA, :categoryB, :price, :productName)")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Product> todayPopularProductReader() {
        return new JdbcCursorItemReaderBuilder<Product>()
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new ProductRowMapper())
                .sql("SELECT * FROM popular_products")
                .name("todayPopularProductReader")
                .build();
    }

    private ItemWriter<Product> todayPopularProductWriter() { // Redis 에 저장하기
        return list -> {
            String key = prefix + new SimpleDateFormat("yyyyMMdd").format(new Date());
            redisTemplate.delete(key);
            redisTemplate.opsForValue().setIfAbsent(key, list);
            redisTemplate.expire(key, Duration.ofHours(24));
        };
    }
}
