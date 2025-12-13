package com.smartdelivery.tracking.controller;

import com.smartdelivery.tracking.model.LocationUpdate;
import com.smartdelivery.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final TrackingService trackingService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Endpoint pour les mises à jour de position depuis l'application mobile du livreur
     * @param deliveryId ID de la livraison
     * @param locationUpdate Mise à jour de position
     */
    @MessageMapping("/tracking/{deliveryId}/location")
    public void updateLocation(@DestinationVariable UUID deliveryId, @Payload LocationUpdate locationUpdate) {
        log.info("Received location update for delivery {}: {}", deliveryId, locationUpdate);

        try {
            // Mettre à jour la position dans la base de données
            trackingService.updateLocation(deliveryId, locationUpdate.getLocation());

            // Envoyer une confirmation au livreur
            messagingTemplate.convertAndSend("/topic/courier/" + locationUpdate.getCourierId() + "/ack", 
                "Location updated successfully");

        } catch (Exception e) {
            log.error("Error processing location update for delivery {}", deliveryId, e);

            // Envoyer un message d'erreur au livreur
            messagingTemplate.convertAndSend("/topic/courier/" + locationUpdate.getCourierId() + "/error", 
                "Failed to update location: " + e.getMessage());
        }
    }
}
