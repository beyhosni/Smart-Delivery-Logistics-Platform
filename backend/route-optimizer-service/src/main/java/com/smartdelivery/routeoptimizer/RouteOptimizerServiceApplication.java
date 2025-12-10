package com.smartdelivery.routeoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class RouteOptimizerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RouteOptimizerServiceApplication.class, args);
    }
}
