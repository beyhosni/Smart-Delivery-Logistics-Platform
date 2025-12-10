package com.smartdelivery.delivery.controller;

import com.smartdelivery.delivery.model.Delivery;
import com.smartdelivery.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries/stats")
@RequiredArgsConstructor
@Slf4j
public class DeliveryStatsController {

    private final DeliveryRepository deliveryRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDeliveryStats() {
        log.info("Getting delivery statistics");

        Map<String, Object> stats = new HashMap<>();

        // Compter les livraisons par statut
        long totalDeliveries = deliveryRepository.count();
        long createdDeliveries = deliveryRepository.countByStatus(Delivery.DeliveryStatus.CREATED);
        long assignedDeliveries = deliveryRepository.countByStatus(Delivery.DeliveryStatus.ASSIGNED);
        long inTransitDeliveries = deliveryRepository.countByStatus(Delivery.DeliveryStatus.IN_TRANSIT);
        long deliveredDeliveries = deliveryRepository.countByStatus(Delivery.DeliveryStatus.DELIVERED);
        long cancelledDeliveries = deliveryRepository.countByStatus(Delivery.DeliveryStatus.CANCELLED);

        // Compter les livraisons par priorité
        long lowPriorityDeliveries = deliveryRepository.countByPriority(Delivery.DeliveryPriority.LOW);
        long normalPriorityDeliveries = deliveryRepository.countByPriority(Delivery.DeliveryPriority.NORMAL);
        long highPriorityDeliveries = deliveryRepository.countByPriority(Delivery.DeliveryPriority.HIGH);
        long urgentPriorityDeliveries = deliveryRepository.countByPriority(Delivery.DeliveryPriority.URGENT);

        // Compter les livraisons par jour (7 derniers jours)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long deliveriesLastWeek = deliveryRepository.countByCreatedAtAfter(weekAgo);

        // Livraison la plus rapide
        // Note: Dans un vrai projet, nous aurions une table dédiée pour stocker les temps de livraison
        // Pour cet exemple, nous simulons une valeur
        double fastestDeliveryHours = 2.5;

        stats.put("totalDeliveries", totalDeliveries);
        stats.put("createdDeliveries", createdDeliveries);
        stats.put("assignedDeliveries", assignedDeliveries);
        stats.put("inTransitDeliveries", inTransitDeliveries);
        stats.put("deliveredDeliveries", deliveredDeliveries);
        stats.put("cancelledDeliveries", cancelledDeliveries);
        stats.put("pendingDeliveries", createdDeliveries + assignedDeliveries);
        stats.put("completedDeliveries", deliveredDeliveries);

        stats.put("lowPriorityDeliveries", lowPriorityDeliveries);
        stats.put("normalPriorityDeliveries", normalPriorityDeliveries);
        stats.put("highPriorityDeliveries", highPriorityDeliveries);
        stats.put("urgentPriorityDeliveries", urgentPriorityDeliveries);

        stats.put("deliveriesLastWeek", deliveriesLastWeek);
        stats.put("fastestDeliveryHours", fastestDeliveryHours);

        // Calculer le taux de livraison réussie
        double deliveryRate = totalDeliveries > 0 ? 
            (double) deliveredDeliveries / totalDeliveries * 100 : 0;
        stats.put("deliveryRate", Math.round(deliveryRate * 100.0) / 100.0);

        return ResponseEntity.ok(stats);
    }
}
