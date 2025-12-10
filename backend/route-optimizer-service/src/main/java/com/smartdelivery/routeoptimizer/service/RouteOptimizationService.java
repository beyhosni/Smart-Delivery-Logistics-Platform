package com.smartdelivery.routeoptimizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdelivery.routeoptimizer.model.Route;
import com.smartdelivery.routeoptimizer.model.RoutePoint;
import com.smartdelivery.routeoptimizer.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizationService {

    private final RouteRepository routeRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${graphhopper.base.url}")
    private String graphhopperBaseUrl;

    /**
     * Crée une route optimisée entre deux points
     * @param deliveryId ID de la livraison
     * @param courierId ID du livreur
     * @param startLatitude Latitude du point de départ
     * @param startLongitude Longitude du point de départ
     * @param endLatitude Latitude du point de destination
     * @param endLongitude Longitude du point de destination
     * @return La route optimisée
     */
    public Route createOptimizedRoute(UUID deliveryId, UUID courierId, 
                                     Double startLatitude, Double startLongitude,
                                     Double endLatitude, Double endLongitude) {
        try {
            // Appel à l'API GraphHopper pour obtenir l'itinéraire
            String url = graphhopperBaseUrl + "/route?point=" + startLatitude + "," + startLongitude +
                         "&point=" + endLatitude + "," + endLongitude +
                         "&vehicle=car&locale=fr&points_encoded=false";

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response == null || !response.has("paths") || response.get("paths").size() == 0) {
                throw new RuntimeException("Impossible de calculer l'itinéraire");
            }

            JsonNode path = response.get("paths").get(0);
            Double distance = path.get("distance").asDouble(); // en mètres
            Long duration = path.get("time").asLong() / 1000; // conversion en secondes

            // Extraire les points de l'itinéraire
            List<RoutePoint> routePoints = new ArrayList<>();
            JsonNode points = path.get("points").get("coordinates");

            for (int i = 0; i < points.size(); i++) {
                JsonNode point = points.get(i);
                RoutePoint routePoint = RoutePoint.builder()
                        .longitude(point.get(0).asDouble())
                        .latitude(point.get(1).asDouble())
                        .estimatedArrival(i == 0 ? 0 : (long) (duration * i / points.size()))
                        .isWaypoint(i > 0 && i < points.size() - 1)
                        .build();

                routePoints.add(routePoint);
            }

            // Calculer l'heure d'arrivée estimée
            LocalDateTime estimatedArrivalTime = LocalDateTime.now().plusSeconds(duration);

            // Créer et sauvegarder la route
            Route route = Route.builder()
                    .deliveryId(deliveryId)
                    .courierId(courierId)
                    .points(routePoints)
                    .totalDistance(distance)
                    .totalDuration(duration)
                    .estimatedArrivalTime(estimatedArrivalTime)
                    .status(Route.RouteStatus.PLANNED)
                    .build();

            return routeRepository.save(route);
        } catch (Exception e) {
            log.error("Erreur lors de la création de la route optimisée", e);
            throw new RuntimeException("Erreur lors de la création de la route optimisée", e);
        }
    }

    /**
     * Optimise une route existante en tenant compte du trafic en temps réel
     * @param routeId ID de la route à optimiser
     * @return La route mise à jour
     */
    public Route optimizeExistingRoute(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route non trouvée avec l'ID: " + routeId));

        if (route.getPoints().size() < 2) {
            return route; // Pas assez de points pour optimiser
        }

        try {
            // Obtenir le premier et le dernier point
            RoutePoint startPoint = route.getPoints().get(0);
            RoutePoint endPoint = route.getPoints().get(route.getPoints().size() - 1);

            // Recalculer l'itinéraire avec les données de trafic actuelles
            String url = graphhopperBaseUrl + "/route?point=" + startPoint.getLatitude() + "," + startPoint.getLongitude() +
                         "&point=" + endPoint.getLatitude() + "," + endPoint.getLongitude() +
                         "&vehicle=car&locale=fr&points_encoded=false&calc_points=true";

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response == null || !response.has("paths") || response.get("paths").size() == 0) {
                log.warn("Impossible de recalculer l'itinéraire pour la route {}", routeId);
                return route;
            }

            JsonNode path = response.get("paths").get(0);
            Double distance = path.get("distance").asDouble(); // en mètres
            Long duration = path.get("time").asLong() / 1000; // conversion en secondes

            // Mettre à jour la route
            route.setTotalDistance(distance);
            route.setTotalDuration(duration);
            route.setEstimatedArrivalTime(LocalDateTime.now().plusSeconds(duration));
            route.setUpdatedAt(LocalDateTime.now());

            return routeRepository.save(route);
        } catch (Exception e) {
            log.error("Erreur lors de l'optimisation de la route {}", routeId, e);
            return route;
        }
    }

    /**
     * Calcule le pourcentage de progression sur une route
     * @param routeId ID de la route
     * @param currentLatitude Latitude actuelle
     * @param currentLongitude Longitude actuelle
     * @return Pourcentage de progression (0.0 - 100.0)
     */
    public Double calculateProgress(UUID routeId, Double currentLatitude, Double currentLongitude) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route non trouvée avec l'ID: " + routeId));

        if (route.getPoints().size() < 2) {
            return 0.0;
        }

        try {
            // Calculer la distance totale de la route
            Double totalDistance = route.getTotalDistance();

            // Calculer la distance restante depuis la position actuelle jusqu'à la destination
            RoutePoint lastPoint = route.getPoints().get(route.getPoints().size() - 1);

            String url = graphhopperBaseUrl + "/route?point=" + currentLatitude + "," + currentLongitude +
                         "&point=" + lastPoint.getLatitude() + "," + lastPoint.getLongitude() +
                         "&vehicle=car&locale=fr";

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response == null || !response.has("paths") || response.get("paths").size() == 0) {
                return 0.0;
            }

            Double remainingDistance = response.get("paths").get(0).get("distance").asDouble();

            // Calculer le pourcentage de progression
            Double progress = (totalDistance - remainingDistance) / totalDistance * 100.0;

            // S'assurer que le pourcentage est entre 0 et 100
            return Math.max(0.0, Math.min(100.0, progress));
        } catch (Exception e) {
            log.error("Erreur lors du calcul de la progression pour la route {}", routeId, e);
            return 0.0;
        }
    }

    /**
     * Met à jour le statut d'une route
     * @param routeId ID de la route
     * @param status Nouveau statut
     * @return La route mise à jour
     */
    public Route updateRouteStatus(UUID routeId, Route.RouteStatus status) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route non trouvée avec l'ID: " + routeId));

        route.setStatus(status);
        route.setUpdatedAt(LocalDateTime.now());

        return routeRepository.save(route);
    }

    /**
     * Récupère la route active d'un livreur
     * @param courierId ID du livreur
     * @return La route active ou null si aucune route active
     */
    public Route getActiveRouteByCourierId(UUID courierId) {
        return routeRepository.findActiveRouteByCourierId(courierId);
    }

    /**
     * Annule toutes les routes actives d'un livreur
     * @param courierId ID du livreur
     * @return La liste des routes annulées
     */
    public List<Route> cancelAllActiveRoutesByCourierId(UUID courierId) {
        List<Route> activeRoutes = routeRepository.findByCourierIdAndStatus(courierId, Route.RouteStatus.ACTIVE);
        List<Route> cancelledRoutes = new ArrayList<>();

        for (Route route : activeRoutes) {
            route.setStatus(Route.RouteStatus.CANCELLED);
            route.setUpdatedAt(LocalDateTime.now());
            cancelledRoutes.add(routeRepository.save(route));
        }

        return cancelledRoutes;
    }
}
