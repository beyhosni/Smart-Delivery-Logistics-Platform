package com.smartdelivery.tracking.controller;

import com.smartdelivery.tracking.model.DeliveryTracking;
import com.smartdelivery.tracking.model.Location;
import com.smartdelivery.tracking.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Slf4j
public class TrackingController {

    private final TrackingService trackingService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/{deliveryId}")
    public ResponseEntity<DeliveryTracking> createTracking(
            @PathVariable UUID deliveryId,
            @RequestParam UUID courierId,
            @Valid @RequestBody Location initialLocation) {
        log.info("Creating tracking for delivery ID: {}", deliveryId);
        DeliveryTracking tracking = trackingService.createTracking(deliveryId, courierId, initialLocation);
        return ResponseEntity.ok(tracking);
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryTracking> getTrackingByDeliveryId(@PathVariable UUID deliveryId) {
        log.info("Getting tracking for delivery ID: {}", deliveryId);
        DeliveryTracking tracking = trackingService.getTrackingByDeliveryId(deliveryId);
        return ResponseEntity.ok(tracking);
    }

    @GetMapping("/courier/{courierId}")
    public ResponseEntity<List<DeliveryTracking>> getTrackingByCourierId(@PathVariable UUID courierId) {
        log.info("Getting tracking for courier ID: {}", courierId);
        List<DeliveryTracking> trackingList = trackingService.getTrackingByCourierId(courierId);
        return ResponseEntity.ok(trackingList);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DeliveryTracking>> getTrackingByStatus(@PathVariable DeliveryTracking.TrackingStatus status) {
        log.info("Getting tracking with status: {}", status);
        List<DeliveryTracking> trackingList = trackingService.getTrackingByStatus(status);
        return ResponseEntity.ok(trackingList);
    }

    @PutMapping("/{deliveryId}/location")
    public ResponseEntity<DeliveryTracking> updateLocation(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody Location location) {
        log.info("Updating location for delivery ID: {}", deliveryId);
        DeliveryTracking tracking = trackingService.updateLocation(deliveryId, location);
        return ResponseEntity.ok(tracking);
    }

    @PutMapping("/{deliveryId}/status")
    public ResponseEntity<DeliveryTracking> updateStatus(
            @PathVariable UUID deliveryId,
            @RequestParam DeliveryTracking.TrackingStatus status) {
        log.info("Updating status to {} for delivery ID: {}", status, deliveryId);
        DeliveryTracking tracking = trackingService.updateTrackingStatus(deliveryId, status);
        return ResponseEntity.ok(tracking);
    }

    // WebSocket endpoint pour les mises à jour en temps réel
    @MessageMapping("/location/update")
    public void handleLocationUpdate(@Payload LocationUpdateMessage message) {
        log.info("Received location update via WebSocket for delivery ID: {}", message.getDeliveryId());
        trackingService.updateLocation(message.getDeliveryId(), message.getLocation());
    }

    // Classe interne pour les messages WebSocket
    public static class LocationUpdateMessage {
        private UUID deliveryId;
        private Location location;

        public UUID getDeliveryId() {
            return deliveryId;
        }

        public void setDeliveryId(UUID deliveryId) {
            this.deliveryId = deliveryId;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }
}
