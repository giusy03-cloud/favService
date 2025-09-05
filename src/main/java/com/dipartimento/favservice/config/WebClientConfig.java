package com.dipartimento.favservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {


    @Bean(name = "userClient")
    public WebClient userClient() {
        return WebClient.builder()
                .baseUrl("http://192.168.0.107:8080")  // indirizzo IP reale della tua macchina + porta userService
                .build();
    }


    @Bean(name = "eventWebClient")
    public WebClient eventClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8081")    // indirizzo event microservice
                .build();
    }
}
