package com.smartdelivery.tracking.repository;

import com.smartdelivery.tracking.model.TrackingEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrackingEventRepository extends MongoRepository<TrackingEvent, String> {

    /**
     * Trouve tous les événements de suivi pour une livraison, triés par date (plus récent en premier)
     * @param deliveryId ID de la livraison
     * @return Liste des événements de suivi
     */
    List<TrackingEvent> findByDeliveryIdOrderByTimestampDesc(UUID deliveryId);

    /**
     * Trouve tous les événements de suivi pour une livraison dans une période donnée
     * @param deliveryId ID de la livraison
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste des événements de suivi
     */
    List<TrackingEvent> findByDeliveryIdAndTimestampBetweenOrderByTimestampDesc(
            UUID deliveryId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Trouve le dernier événement de suivi pour une livraison
     * @param deliveryId ID de la livraison
     * @return Le dernier événement de suivi
     */
    @Query(value = "{ 'deliveryId' : ?0 }", sort = "{ 'timestamp' : -1 }")
    TrackingEvent findLatestByDeliveryId(UUID deliveryId);

    /**
     * Trouve tous les événements d'un type spécifique pour une livraison
     * @param deliveryId ID de la livraison
     * @param eventType Type d'événement
     * @return Liste des événements de suivi
     */
    List<TrackingEvent> findByDeliveryIdAndEventTypeOrderByTimestampDesc(
            UUID deliveryId, TrackingEvent.TrackingEventType eventType);
}
