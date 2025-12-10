package com.smartdelivery.tracking.service;

import com.smartdelivery.tracking.model.DeliveryTracking;
import com.smartdelivery.tracking.model.Location;
import com.smartdelivery.tracking.repository.DeliveryTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingService {

    private final DeliveryTrackingRepository trackingRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final RouteOptimizerService routeOptimizerService;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    public DeliveryTracking createTracking(UUID deliveryId, UUID courierId, Location initialLocation) {
        DeliveryTracking tracking = DeliveryTracking.builder()
                .deliveryId(deliveryId)
                .courierId(courierId)
                .currentLocation(initialLocation)
                .status(DeliveryTracking.TrackingStatus.DISPATCHED)
                .lastUpdated(LocalDateTime.now())
                .progressPercentage(0.0)
                .build();

        tracking.addLocationToHistory(initialLocation);

        DeliveryTracking savedTracking = trackingRepository.save(tracking);

        // Notifier les clients via WebSocket
        messagingTemplate.convertAndSend("/topic/tracking/" + deliveryId, savedTracking);

        log.info("Created tracking for delivery ID: {}", deliveryId);
        return savedTracking;
    }

    public DeliveryTracking updateLocation(UUID deliveryId, Location newLocation) {
        DeliveryTracking tracking = trackingRepository.findByDeliveryId(deliveryId);

        if (tracking == null) {
            throw new RuntimeException("Tracking not found for delivery ID: " + deliveryId);
        }

        // Mettre à jour la position actuelle
        tracking.setCurrentLocation(newLocation);
        tracking.addLocationToHistory(newLocation);

        // Calculer le pourcentage de progression
        updateProgressPercentage(tracking);

        // Mettre à jour le statut si nécessaire
        updateTrackingStatus(tracking);

        DeliveryTracking savedTracking = trackingRepository.save(tracking);

        // Notifier les clients via WebSocket
        messagingTemplate.convertAndSend("/topic/tracking/" + deliveryId, savedTracking);

        // Publier l'événement de mise à jour
        publishLocationUpdateEvent(savedTracking);

        log.info("Updated location for delivery ID: {}", deliveryId);
        return savedTracking;
    }

    public DeliveryTracking getTrackingByDeliveryId(UUID deliveryId) {
        return trackingRepository.findByDeliveryId(deliveryId);
    }

    public List<DeliveryTracking> getTrackingByCourierId(UUID courierId) {
        return trackingRepository.findByCourierId(courierId);
    }

    public List<DeliveryTracking> getTrackingByStatus(DeliveryTracking.TrackingStatus status) {
        return trackingRepository.findByStatus(status);
    }

    public DeliveryTracking updateTrackingStatus(UUID deliveryId, DeliveryTracking.TrackingStatus status) {
        DeliveryTracking tracking = trackingRepository.findByDeliveryId(deliveryId);

        if (tracking == null) {
            throw new RuntimeException("Tracking not found for delivery ID: " + deliveryId);
        }

        tracking.setStatus(status);
        tracking.setLastUpdated(LocalDateTime.now());

        DeliveryTracking savedTracking = trackingRepository.save(tracking);

        // Notifier les clients via WebSocket
        messagingTemplate.convertAndSend("/topic/tracking/" + deliveryId, savedTracking);

        // Publier l'événement de changement de statut
        publishStatusUpdateEvent(savedTracking);

        log.info("Updated tracking status to {} for delivery ID: {}", status, deliveryId);
        return savedTracking;
    }

    // Planifié toutes les minutes pour vérifier les livraisons en retard
    @Scheduled(fixedRate = 60000)
    public void checkDelayedDeliveries() {
        List<DeliveryTracking> inTransitDeliveries = trackingRepository.findByStatus(DeliveryTracking.TrackingStatus.IN_TRANSIT);

        LocalDateTime now = LocalDateTime.now();

        for (DeliveryTracking tracking : inTransitDeliveries) {
            if (tracking.getEstimatedDeliveryTime() != null && now.isAfter(tracking.getEstimatedDeliveryTime())) {
                // Notification de livraison en retard
                log.warn("Delivery {} is delayed. Estimated time: {}", tracking.getDeliveryId(), tracking.getEstimatedDeliveryTime());

                // Envoyer une notification via RabbitMQ
                rabbitTemplate.convertAndSend(exchangeName, "delivery.delayed", tracking);
            }
        }
    }

    private void updateProgressPercentage(DeliveryTracking tracking) {
        if (tracking.getRouteId() != null) {
            // Obtenir les informations de la route depuis le service d'optimisation
            Double progress = routeOptimizerService.calculateProgress(tracking.getRouteId(), tracking.getCurrentLocation());
            tracking.setProgressPercentage(progress);
        }
    }

    private void updateTrackingStatus(DeliveryTracking tracking) {
        // Logique pour mettre à jour le statut en fonction de la position
        // Par exemple, si le pourcentage de progression est proche de 100%, marquer comme livré
        if (tracking.getProgressPercentage() != null && tracking.getProgressPercentage() >= 95.0) {
            tracking.setStatus(DeliveryTracking.TrackingStatus.DELIVERED);
        } else if (tracking.getProgressPercentage() != null && tracking.getProgressPercentage() > 0.0) {
            tracking.setStatus(DeliveryTracking.TrackingStatus.IN_TRANSIT);
        }
    }

    private void publishLocationUpdateEvent(DeliveryTracking tracking) {
        rabbitTemplate.convertAndSend(exchangeName, "location.updated", tracking);
        log.info("Published location.updated event for delivery ID: {}", tracking.getDeliveryId());
    }

    private void publishStatusUpdateEvent(DeliveryTracking tracking) {
        rabbitTemplate.convertAndSend(exchangeName, "tracking.status.updated", tracking);
        log.info("Published tracking.status.updated event for delivery ID: {}", tracking.getDeliveryId());
    }
}
