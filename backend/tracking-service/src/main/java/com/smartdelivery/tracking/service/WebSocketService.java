package com.smartdelivery.tracking.service;

import com.smartdelivery.tracking.model.DeliveryTracking;
import com.smartdelivery.tracking.model.LocationUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifie les clients d'une mise à jour de position
     * @param deliveryId ID de la livraison
     * @param locationUpdate Mise à jour de position
     */
    public void notifyLocationUpdate(UUID deliveryId, LocationUpdate locationUpdate) {
        String topic = "/topic/tracking/" + deliveryId + "/location";
        messagingTemplate.convertAndSend(topic, locationUpdate);
        log.info("Sent location update to topic {} for delivery {}", topic, deliveryId);
    }

    /**
     * Notifie les clients d'une mise à jour du suivi
     * @param tracking Suivi mis à jour
     */
    public void notifyTrackingUpdate(DeliveryTracking tracking) {
        String topic = "/topic/tracking/" + tracking.getDeliveryId();
        messagingTemplate.convertAndSend(topic, tracking);
        log.info("Sent tracking update to topic {} for delivery {}", topic, tracking.getDeliveryId());
    }

    /**
     * Notifie les clients d'une mise à jour de statut
     * @param deliveryId ID de la livraison
     * @param status Nouveau statut
     */
    public void notifyStatusUpdate(UUID deliveryId, String status) {
        String topic = "/topic/tracking/" + deliveryId + "/status";
        messagingTemplate.convertAndSend(topic, status);
        log.info("Sent status update to topic {} for delivery {}", topic, deliveryId);
    }
}
