package com.example.batch.Product.job;

import com.example.batch.Product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@RequiredArgsConstructor
@Configuration
public class ProductManagementJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private final int chunkSize = 100;

    @Bean
    public Job productManagementJob() {
        return jobBuilderFactory.get("productManagementJob")
                .start(productManagementStep())
                .build();
    }

    @Bean
    public Step productManagementStep() {
        return stepBuilderFactory.get("productManagementStep")
                .<Product, Product>chunk(chunkSize)
                .reader(productItemReader())
                .processor(productItemProcessor())
                .writer(productItemWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Product> productItemReader() {
        JpaPagingItemReader<Product> reader = new JpaPagingItemReader<Product>() {
            @Override
            public int getPage() {
                return 0;
            }
        };

        reader.setName("productManagementReader");
        reader.setEntityManagerFactory(entityManagerFactory);
        reader.setPageSize(chunkSize);
        reader.setQueryString("SELECT p FROM Product p WHERE p.amount = 0");

        return reader;
    }

    @Bean
    public ItemProcessor<Product, Product> productItemProcessor() {
        return product -> {
            product.setAmount(1000);
            return product;
        };
    }

    @Bean
    public JpaItemWriter<Product> productItemWriter() {
        return new JpaItemWriterBuilder<Product>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
