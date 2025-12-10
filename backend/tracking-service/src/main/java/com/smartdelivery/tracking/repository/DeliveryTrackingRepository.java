package com.smartdelivery.tracking.repository;

import com.smartdelivery.tracking.model.DeliveryTracking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryTrackingRepository extends MongoRepository<DeliveryTracking, String> {

    DeliveryTracking findByDeliveryId(UUID deliveryId);

    List<DeliveryTracking> findByCourierId(UUID courierId);

    List<DeliveryTracking> findByStatus(DeliveryTracking.TrackingStatus status);

    @Query("{ 'deliveryId': ?0, 'locationHistory.timestamp': { $gte: ?1, $lte: ?2 } }")
    List<DeliveryTracking> findByDeliveryIdAndLocationHistoryTimestampBetween(
            UUID deliveryId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("{ 'courierId': ?0, 'lastUpdated': { $gte: ?1 } }")
    List<DeliveryTracking> findByCourierIdAndLastUpdatedAfter(UUID courierId, LocalDateTime lastUpdated);

    @Query(value = "{ 'currentLocation': { $near: { $geometry: { type: 'Point', coordinates: [ ?0, ?1 ] }, $maxDistance: ?2 } } }", 
           count = true)
    long countDeliveriesNearLocation(Double longitude, Double latitude, Double maxDistanceInMeters);
}
