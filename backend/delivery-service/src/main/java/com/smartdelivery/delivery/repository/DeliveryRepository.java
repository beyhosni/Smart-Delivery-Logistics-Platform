package com.smartdelivery.delivery.repository;

import com.smartdelivery.delivery.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    List<Delivery> findBySenderId(UUID senderId);

    List<Delivery> findByRecipientId(UUID recipientId);

    List<Delivery> findByStatus(Delivery.DeliveryStatus status);

    @Query("SELECT d FROM Delivery d WHERE d.requestedDeliveryTime BETWEEN :startDate AND :endDate")
    List<Delivery> findByRequestedDeliveryTimeBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM Delivery d WHERE d.senderId = :userId OR d.recipientId = :userId")
    List<Delivery> findByUserId(@Param("userId") UUID userId);
}
