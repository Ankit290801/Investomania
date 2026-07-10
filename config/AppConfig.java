package com.investment.tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application configuration
 */
@Configuration
public class AppConfig {
    
    /**
     * RestTemplate bean for making HTTP requests to external APIs
     * (Yahoo Finance, Google Finance, ExchangeRate API, etc.)
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
