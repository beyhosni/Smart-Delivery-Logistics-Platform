package com.smartdelivery.tracking.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdelivery.tracking.model.DeliveryTracking;
import com.smartdelivery.tracking.model.Location;
import com.smartdelivery.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventListener {

    private final TrackingService trackingService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.dispatched.name}")
    public void handleDeliveryDispatched(Map<String, Object> message) {
        try {
            log.info("Received delivery.dispatched event");

            // Extraire les informations du message
            UUID deliveryId = UUID.fromString(message.get("deliveryId").toString());
            UUID courierId = UUID.fromString(message.get("courierId").toString());

            // Obtenir la position de départ (adresse de ramassage)
            Map<String, Object> pickupAddress = (Map<String, Object>) message.get("pickupAddress");
            Map<String, Object> coordinates = (Map<String, Object>) pickupAddress.get("coordinates");

            Location pickupLocation = Location.builder()
                    .latitude(Double.parseDouble(coordinates.get("latitude").toString()))
                    .longitude(Double.parseDouble(coordinates.get("longitude").toString()))
                    .address(pickupAddress.get("street").toString() + ", " + 
                            pickupAddress.get("city").toString())
                    .build();

            // Créer le suivi pour cette livraison
            trackingService.createTracking(deliveryId, courierId, pickupLocation);

        } catch (Exception e) {
            log.error("Error processing delivery.dispatched event", e);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.in_transit.name}")
    public void handleDeliveryInTransit(Map<String, Object> message) {
        try {
            log.info("Received delivery.in_transit event");

            // Extraire les informations du message
            UUID deliveryId = UUID.fromString(message.get("deliveryId").toString());
            UUID courierId = UUID.fromString(message.get("courierId").toString());

            // Obtenir la position actuelle
            Map<String, Object> currentLocationMap = (Map<String, Object>) message.get("currentLocation");

            Location currentLocation = Location.builder()
                    .latitude(Double.parseDouble(currentLocationMap.get("latitude").toString()))
                    .longitude(Double.parseDouble(currentLocationMap.get("longitude").toString()))
                    .build();

            // Mettre à jour la position
            trackingService.updateLocation(deliveryId, currentLocation);

            // Mettre à jour le statut si nécessaire
            trackingService.updateTrackingStatus(deliveryId, DeliveryTracking.TrackingStatus.IN_TRANSIT);

        } catch (Exception e) {
            log.error("Error processing delivery.in_transit event", e);
        }
    }
}
