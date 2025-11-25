package com.example.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class IntegrationTestingApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestingApplication.class, args);
    }
}