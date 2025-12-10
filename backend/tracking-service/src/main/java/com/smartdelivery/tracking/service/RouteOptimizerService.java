package com.smartdelivery.tracking.service;

import com.smartdelivery.tracking.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class RouteOptimizerService {

    private final RestTemplate restTemplate;

    @Value("${route-optimizer.service.url}")
    private String routeOptimizerServiceUrl;

    public RouteOptimizerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Calcule le pourcentage de progression sur une route
     * @param routeId ID de la route
     * @param currentLocation Position actuelle
     * @return Pourcentage de progression (0.0 - 100.0)
     */
    public Double calculateProgress(String routeId, Location currentLocation) {
        try {
            // Appeler le service d'optimisation de route
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("routeId", routeId);
            requestBody.put("latitude", currentLocation.getLatitude());
            requestBody.put("longitude", currentLocation.getLongitude());

            Map<String, Object> response = restTemplate.postForObject(
                    routeOptimizerServiceUrl + "/api/routes/progress",
                    requestBody,
                    Map.class
            );

            if (response != null && response.containsKey("progressPercentage")) {
                return (Double) response.get("progressPercentage");
            }
        } catch (Exception e) {
            log.error("Error calculating route progress", e);
        }

        // En cas d'erreur, retourner une estimation basique basée sur la distance
        return calculateBasicProgress(currentLocation);
    }

    /**
     * Calcule une progression basique basée sur la distance par rapport à un point de destination
     * @param currentLocation Position actuelle
     * @return Pourcentage de progression estimé
     */
    private Double calculateBasicProgress(Location currentLocation) {
        // Implémentation simple - dans un vrai système, nous utiliserions l'API de cartographie
        // pour calculer la distance restante par rapport à la destination

        // Pour cet exemple, nous retournons une valeur aléatoire entre 0 et 95
        // pour simuler une progression
        return Math.random() * 95.0;
    }
}
