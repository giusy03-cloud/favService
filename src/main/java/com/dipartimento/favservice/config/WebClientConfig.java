package com.dipartimento.favservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "userClient")
    public WebClient userClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8080")  // indirizzo user microservice
                .build();
    }

    @Bean(name = "eventWebClient")
    public WebClient eventClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8081")    // indirizzo event microservice
                .build();
    }
}
