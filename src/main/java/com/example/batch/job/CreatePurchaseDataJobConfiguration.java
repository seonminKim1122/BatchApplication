package com.example.batch.job;

import com.example.batch.entity.User;
import com.example.batch.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RequiredArgsConstructor
@Configuration
public class CreatePurchaseDataJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ProductRepository productRepository;

    @Bean
    public Job createPurchaseDataJob() {
        return jobBuilderFactory.get("createPurchaseDataJob")
                .start(createPurchaseDataStep())
                .build();
    }

    @Bean
    public Step createPurchaseDataStep() {
        String password = "testPassword";
        WebClient webClient = WebClient.builder()
                .baseUrl("http://HotdealLoadBalancer-1501345169.ap-northeast-2.elb.amazonaws.com")
                .build();
        long productCnt = productRepository.getCount();
        Random random = new Random();

        return stepBuilderFactory.get("createPurchaseDataStep")
                .tasklet((contribution, chunkContext) -> {
                    for (int i = 1; i < 501; i++) {
                        String userId = "seonmin" + i;
                        User user = new User(userId, password);
                        ClientResponse response = webClient.post()
                                .uri("/users/login")
                                .bodyValue(user)
                                .exchange().block();

                        String token = response.headers().asHttpHeaders().get("Authorization").get(0);
                        long productId = random.nextLong(productCnt)+1;
                        Map<String, Integer> body = new HashMap<>();
                        body.put("quantity", random.nextInt(10)+1);

                        String res = webClient.post()
                                .uri("/products/{productId}", productId)
                                .header("Authorization", token)
                                .bodyValue(body)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();

                        System.out.println(res);
                    }
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
