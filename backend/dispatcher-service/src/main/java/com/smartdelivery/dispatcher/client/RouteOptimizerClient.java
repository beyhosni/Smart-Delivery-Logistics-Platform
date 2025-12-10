package com.smartdelivery.dispatcher.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RouteOptimizerClient {

    private final RestTemplate restTemplate;

    @Value("${route-optimizer-service.url}")
    private String routeOptimizerServiceUrl;

    public RouteOptimizerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Crée une route optimisée via le route-optimizer-service
     * @param deliveryId ID de la livraison
     * @param courierId ID du livreur
     * @param startLatitude Latitude du point de départ
     * @param startLongitude Longitude du point de départ
     * @param endLatitude Latitude du point de destination
     * @param endLongitude Longitude du point de destination
     * @return ID de la route créée
     */
    public UUID createOptimizedRoute(UUID deliveryId, UUID courierId,
                                   Double startLatitude, Double startLongitude,
                                   Double endLatitude, Double endLongitude) {
        String url = routeOptimizerServiceUrl + "/api/routes";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("deliveryId", deliveryId);
        requestBody.put("courierId", courierId);
        requestBody.put("startLatitude", startLatitude);
        requestBody.put("startLongitude", startLongitude);
        requestBody.put("endLatitude", endLatitude);
        requestBody.put("endLongitude", endLongitude);

        Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

        if (response != null && response.containsKey("id")) {
            return UUID.fromString(response.get("id").toString());
        }

        throw new RuntimeException("Impossible de créer la route optimisée");
    }

    /**
     * Met à jour le statut d'une route
     * @param routeId ID de la route
     * @param status Nouveau statut
     * @return La route mise à jour
     */
    public Map<String, Object> updateRouteStatus(UUID routeId, String status) {
        String url = routeOptimizerServiceUrl + "/api/routes/" + routeId + "/status";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("status", status);

        return restTemplate.postForObject(url, requestBody, Map.class);
    }
}
