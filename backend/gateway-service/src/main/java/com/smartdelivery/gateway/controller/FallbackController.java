package com.smartdelivery.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/delivery-service")
    public Mono<Map<String, String>> deliveryServiceFallback() {
        return Mono.just(createFallbackResponse("Delivery Service"));
    }

    @RequestMapping("/fallback/dispatcher-service")
    public Mono<Map<String, String>> dispatcherServiceFallback() {
        return Mono.just(createFallbackResponse("Dispatcher Service"));
    }

    @RequestMapping("/fallback/tracking-service")
    public Mono<Map<String, String>> trackingServiceFallback() {
        return Mono.just(createFallbackResponse("Tracking Service"));
    }

    @RequestMapping("/fallback/notification-service")
    public Mono<Map<String, String>> notificationServiceFallback() {
        return Mono.just(createFallbackResponse("Notification Service"));
    }

    @RequestMapping("/fallback/route-optimizer-service")
    public Mono<Map<String, String>> routeOptimizerServiceFallback() {
        return Mono.just(createFallbackResponse("Route Optimizer Service"));
    }

    private Map<String, String> createFallbackResponse(String serviceName) {
        Map<String, String> response = new HashMap<>();
        response.put("message", serviceName + " is currently unavailable. Please try again later.");
        response.put("status", "SERVICE_UNAVAILABLE");
        return response;
    }
}
