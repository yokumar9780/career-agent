package com.careeragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the Career Agent service.
 */
@SpringBootApplication
@EnableScheduling
public class CareerAgentApplication {

    static void main(String[] args) {
        SpringApplication.run(CareerAgentApplication.class, args);
    }
}
