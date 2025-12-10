package com.smartdelivery.dispatcher.repository;

import com.smartdelivery.dispatcher.model.DeliveryAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    List<DeliveryAssignment> findByCourierId(UUID courierId);

    List<DeliveryAssignment> findByDeliveryId(UUID deliveryId);

    List<DeliveryAssignment> findByStatus(DeliveryAssignment.AssignmentStatus status);

    @Query("SELECT COUNT(a) FROM DeliveryAssignment a WHERE a.courier.id = :courierId AND a.status = 'IN_PROGRESS'")
    int countActiveAssignmentsByCourierId(@Param("courierId") UUID courierId);

    @Query("SELECT a FROM DeliveryAssignment a WHERE a.courier.id = :courierId AND a.status = 'IN_PROGRESS'")
    List<DeliveryAssignment> findActiveAssignmentsByCourierId(@Param("courierId") UUID courierId);

    @Query("SELECT a FROM DeliveryAssignment a WHERE a.assignedAt BETWEEN :startDate AND :endDate")
    List<DeliveryAssignment> findByAssignedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
