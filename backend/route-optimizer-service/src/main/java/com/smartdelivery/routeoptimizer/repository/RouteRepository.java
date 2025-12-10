package com.smartdelivery.routeoptimizer.repository;

import com.smartdelivery.routeoptimizer.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {

    List<Route> findByDeliveryId(UUID deliveryId);

    List<Route> findByCourierId(UUID courierId);

    List<Route> findByStatus(Route.RouteStatus status);

    @Query("SELECT r FROM Route r WHERE r.courierId = :courierId AND r.status = 'ACTIVE'")
    Route findActiveRouteByCourierId(@Param("courierId") UUID courierId);

    @Query("SELECT r FROM Route r WHERE r.estimatedArrivalTime BETWEEN :startTime AND :endTime")
    List<Route> findRoutesByEstimatedArrivalTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(r) FROM Route r WHERE r.courierId = :courierId AND r.status = 'ACTIVE'")
    int countActiveRoutesByCourierId(@Param("courierId") UUID courierId);
}
