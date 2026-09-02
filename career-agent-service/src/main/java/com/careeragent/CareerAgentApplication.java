package com.careeragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CareerAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerAgentApplication.class, args);
    }
}
