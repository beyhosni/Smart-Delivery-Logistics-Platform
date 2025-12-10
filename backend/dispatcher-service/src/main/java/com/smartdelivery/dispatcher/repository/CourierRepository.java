package com.smartdelivery.dispatcher.repository;

import com.smartdelivery.dispatcher.model.Courier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CourierRepository extends JpaRepository<Courier, UUID> {

    List<Courier> findByStatus(Courier.CourierStatus status);

    @Query("SELECT c FROM Courier c WHERE c.status = 'AVAILABLE' AND c.currentLocation.latitude BETWEEN :minLat AND :maxLat AND c.currentLocation.longitude BETWEEN :minLng AND :maxLng")
    List<Courier> findAvailableCouriersInArea(
            @Param("minLat") Double minLatitude,
            @Param("maxLat") Double maxLatitude,
            @Param("minLng") Double minLongitude,
            @Param("maxLng") Double maxLongitude);

    @Query("SELECT COUNT(c) FROM Courier c WHERE c.status = 'AVAILABLE'")
    int countAvailableCouriers();

    @Query("SELECT c FROM Courier c WHERE c.email = :email")
    Courier findByEmail(@Param("email") String email);
}
