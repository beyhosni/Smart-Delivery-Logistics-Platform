package com.smartdelivery.notification.repository;

import com.smartdelivery.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByDeliveryId(UUID deliveryId);

    List<Notification> findByStatus(Notification.NotificationStatus status);

    List<Notification> findByType(Notification.NotificationType type);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findByUserIdAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = 'FAILED' AND n.createdAt >= :since")
    int countFailedNotificationsSince(@Param("since") LocalDateTime since);

    @Query("SELECT n FROM Notification n WHERE n.channel = :channel AND n.status = 'PENDING'")
    List<Notification> findPendingNotificationsByChannel(@Param("channel") Notification.NotificationChannel channel);
}
