package com.smartdelivery.notification.repository;

import com.smartdelivery.notification.model.PushNotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PushNotificationTokenRepository extends JpaRepository<PushNotificationToken, UUID> {

    List<PushNotificationToken> findByUserId(UUID userId);

    List<PushNotificationToken> findByUserIdAndIsActive(UUID userId, Boolean isActive);

    PushNotificationToken findByDeviceToken(String deviceToken);

    @Query("SELECT pnt FROM PushNotificationToken pnt WHERE pnt.userId = :userId AND pnt.isActive = true AND pnt.platform = :platform")
    List<PushNotificationToken> findByUserIdAndPlatformAndIsActive(
            @Param("userId") UUID userId,
            @Param("platform") PushNotificationToken.Platform platform);

    @Query("SELECT pnt FROM PushNotificationToken pnt WHERE pnt.isActive = true AND pnt.updatedAt < :threshold")
    List<PushNotificationToken> findInactiveTokensOlderThan(@Param("threshold") java.time.LocalDateTime threshold);

    @Query("SELECT pnt FROM PushNotificationToken pnt WHERE pnt.deviceId = :deviceId AND pnt.isActive = true")
    List<PushNotificationToken> findByDeviceIdAndIsActive(@Param("deviceId") String deviceId);
}
