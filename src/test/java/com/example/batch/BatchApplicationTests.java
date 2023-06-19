package com.example.batch;

import com.example.batch.entity.User;
import com.example.batch.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class BatchApplicationTests {

    @Autowired
    private ProductRepository productRepository;


    @Test
    void contextLoads() {
    }

    @Test
    void webClientTests() {
        WebClient webClient =
                WebClient.builder()
                        .baseUrl("http://TestServerLoadBalancer-1450366396.ap-northeast-2.elb.amazonaws.com")
                        .build();

        User user = new User("seonmin1", "testPassword");
        ClientResponse response = webClient.post()
                .uri("/users/login")
                .bodyValue(user)
                .exchange().block();

        String token = response.headers().asHttpHeaders().get("Authorization").get(0);

        long productId = productRepository.getCount();
        Map<String, Object> body = new HashMap<>();
        body.put("quantity", 20);
        String res = webClient.post()
                .uri("/products/{productId}", productId)
                .header("Authorization", token)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println(res);
    }

}
