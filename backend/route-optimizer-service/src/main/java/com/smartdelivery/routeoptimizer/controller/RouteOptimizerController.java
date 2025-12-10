package com.smartdelivery.routeoptimizer.controller;

import com.smartdelivery.routeoptimizer.model.OptimizedRoute;
import com.smartdelivery.routeoptimizer.model.RouteRequest;
import com.smartdelivery.routeoptimizer.service.RouteOptimizerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
public class RouteOptimizerController {

    private final RouteOptimizerService routeOptimizerService;

    @PostMapping("/optimize")
    public ResponseEntity<OptimizedRoute> optimizeRoute(@RequestBody RouteRequest request) {
        log.info("Optimizing route for {} waypoints", request.getWaypoints().size());

        try {
            OptimizedRoute optimizedRoute = routeOptimizerService.optimizeRoute(request);
            return ResponseEntity.ok(optimizedRoute);
        } catch (Exception e) {
            log.error("Error optimizing route", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/matrix")
    public ResponseEntity<Object> getDistanceMatrix(@RequestParam List<String> points) {
        log.info("Calculating distance matrix for {} points", points.size());

        try {
            // Dans un vrai projet, ceci utiliserait GraphHopper Distance Matrix API
            // Pour cet exemple, nous simulons une réponse
            return ResponseEntity.ok("{"message":"Distance matrix calculated successfully"}");
        } catch (Exception e) {
            log.error("Error calculating distance matrix", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/isochrone")
    public ResponseEntity<Object> getIsochrone(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Integer timeLimit) {
        log.info("Calculating isochrone for lat={}, lon={}, timeLimit={}", latitude, longitude, timeLimit);

        try {
            // Dans un vrai projet, ceci utiliserait GraphHopper Isochrone API
            // Pour cet exemple, nous simulons une réponse
            return ResponseEntity.ok("{"message":"Isochrone calculated successfully"}");
        } catch (Exception e) {
            log.error("Error calculating isochrone", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
