package com.smartdelivery.routeoptimizer.service;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.GHUtility;
import com.smartdelivery.routeoptimizer.model.OptimizedRoute;
import com.smartdelivery.routeoptimizer.model.RouteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizerService {

    private final GraphHopper graphHopper;

    public OptimizedRoute optimizeRoute(RouteRequest request) {
        log.info("Optimizing route for {} waypoints", request.getWaypoints().size());

        try {
            // Convertir les points de passage en format GraphHopper
            List<GHUtility> waypoints = new ArrayList<>();
            request.getWaypoints().forEach(point -> {
                waypoints.add(GHUtility.fromDouble(point.getLatitude(), point.getLongitude()));
            });

            // Calculer l'itinéraire optimisé
            Path path = graphHopper.route(waypoints);

            // Créer la réponse avec l'itinéraire optimisé
            OptimizedRoute optimizedRoute = OptimizedRoute.builder()
                    .distance(path.getDistance())
                    .time(path.getTime() / 1000.0) // Convertir en secondes
                    .points(convertPathToPoints(path))
                    .instructions(convertInstructions(path))
                    .build();

            log.info("Route optimized successfully: distance={}, time={}", 
                    optimizedRoute.getDistance(), optimizedRoute.getTime());

            return optimizedRoute;
        } catch (Exception e) {
            log.error("Error optimizing route", e);
            throw new RuntimeException("Failed to optimize route", e);
        }
    }

    private List<com.smartdelivery.routeoptimizer.model.Point> convertPathToPoints(Path path) {
        List<com.smartdelivery.routeoptimizer.model.Point> points = new ArrayList<>();
        if (path.getPoints() != null) {
            path.getPoints().forEach(ghPoint -> {
                points.add(com.smartdelivery.routeoptimizer.model.Point.builder()
                        .latitude(ghPoint.getLat())
                        .longitude(ghPoint.getLon())
                        .build());
            });
        }
        return points;
    }

    private List<String> convertInstructions(Path path) {
        List<String> instructions = new ArrayList<>();
        if (path.getInstructions() != null) {
            path.getInstructions().forEach(instruction -> {
                instructions.add(instruction.getText());
            });
        }
        return instructions;
    }
}
