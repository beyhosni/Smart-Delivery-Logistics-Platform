package com.smartdelivery.dispatcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RouteOptimizerClient {

    private final RestTemplate restTemplate;
    private final String routeOptimizerServiceUrl;

    public RouteOptimizerClient(
            RestTemplate restTemplate,
            @Value("${route.optimizer.service.url:http://route-optimizer-service:8085}") String routeOptimizerServiceUrl) {
        this.restTemplate = restTemplate;
        this.routeOptimizerServiceUrl = routeOptimizerServiceUrl;
    }

    /**
     * Crée une route optimisée pour une livraison
     * @param deliveryId ID de la livraison
     * @param courierId ID du livreur
     * @param pickupLatitude Latitude du point de ramassage
     * @param pickupLongitude Longitude du point de ramassage
     * @param deliveryLatitude Latitude du point de livraison
     * @param deliveryLongitude Longitude du point de livraison
     * @return ID de la route créée
     */
    public UUID createOptimizedRoute(
            UUID deliveryId,
            UUID courierId,
            Double pickupLatitude,
            Double pickupLongitude,
            Double deliveryLatitude,
            Double deliveryLongitude) {

        String url = routeOptimizerServiceUrl + "/api/routes";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deliveryId", deliveryId.toString());
        requestBody.put("courierId", courierId.toString());

        Map<String, Object> pickupLocation = new HashMap<>();
        pickupLocation.put("latitude", pickupLatitude);
        pickupLocation.put("longitude", pickupLongitude);
        requestBody.put("pickupLocation", pickupLocation);

        Map<String, Object> deliveryLocation = new HashMap<>();
        deliveryLocation.put("latitude", deliveryLatitude);
        deliveryLocation.put("longitude", deliveryLongitude);
        requestBody.put("deliveryLocation", deliveryLocation);

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        if (response != null && response.containsKey("routeId")) {
            return UUID.fromString((String) response.get("routeId"));
        }

        throw new RuntimeException("Failed to create optimized route");
    }
}
