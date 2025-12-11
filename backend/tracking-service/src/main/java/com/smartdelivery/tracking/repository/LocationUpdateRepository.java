package com.smartdelivery.tracking.repository;

import com.smartdelivery.tracking.model.LocationUpdate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LocationUpdateRepository extends MongoRepository<LocationUpdate, String> {

    List<LocationUpdate> findByDeliveryIdOrderByTimestampDesc(String deliveryId);

    List<LocationUpdate> findByCourierIdOrderByTimestampDesc(String courierId);

    @Query(value = "{ 'deliveryId': ?0, 'timestamp': { $gte: ?1, $lte: ?2} }", sort = "{ 'timestamp': 1 }")
    List<LocationUpdate> findByDeliveryIdAndTimestampBetween(String deliveryId, LocalDateTime start, LocalDateTime end);

    @Query(value = "{ 'courierId': ?0, 'timestamp': { $gte: ?1, $lte: ?2} }", sort = "{ 'timestamp': 1 }")
    List<LocationUpdate> findByCourierIdAndTimestampBetween(String courierId, LocalDateTime start, LocalDateTime end);

    @Query(value = "{ 'deliveryId': ?0 }", fields = "{ 'latitude': 1, 'longitude': 1, 'timestamp': 1 }", sort = "{ 'timestamp': 1 }")
    List<LocationUpdate> findCoordinatesByDeliveryId(String deliveryId);

    @Query(value = "{ 'courierId': ?0 }", fields = "{ 'latitude': 1, 'longitude': 1, 'timestamp': 1 }", sort = "{ 'timestamp': 1 }")
    List<LocationUpdate> findCoordinatesByCourierId(String courierId);
}
