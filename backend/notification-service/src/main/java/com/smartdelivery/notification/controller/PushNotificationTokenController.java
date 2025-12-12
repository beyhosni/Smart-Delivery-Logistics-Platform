package com.smartdelivery.notification.controller;

import com.smartdelivery.notification.model.PushNotificationToken;
import com.smartdelivery.notification.repository.PushNotificationTokenRepository;
import com.smartdelivery.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/push-tokens")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationTokenController {

    private final PushNotificationTokenRepository tokenRepository;
    private final PushNotificationService pushNotificationService;

    @PostMapping("/register")
    public ResponseEntity<PushNotificationToken> registerPushToken(
            @RequestParam UUID userId,
            @RequestParam String deviceToken,
            @RequestParam PushNotificationToken.Platform platform,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String appVersion,
            @RequestParam(required = false) String osVersion) {

        log.info("Registering push token for user ID: {} and platform: {}", userId, platform);

        // Vérifier si le token existe déjà
        PushNotificationToken existingToken = tokenRepository.findByDeviceToken(deviceToken);

        if (existingToken != null) {
            // Mettre à jour le token existant
            existingToken.setUserId(userId);
            existingToken.setPlatform(platform);
            existingToken.setDeviceId(deviceId);
            existingToken.setAppVersion(appVersion);
            existingToken.setOsVersion(osVersion);
            existingToken.setIsActive(true);

            existingToken = tokenRepository.save(existingToken);
            return ResponseEntity.ok(existingToken);
        }

        // Créer un nouveau token
        PushNotificationToken newToken = PushNotificationToken.builder()
                .userId(userId)
                .deviceToken(deviceToken)
                .platform(platform)
                .deviceId(deviceId)
                .appVersion(appVersion)
                .osVersion(osVersion)
                .isActive(true)
                .build();

        newToken = tokenRepository.save(newToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(newToken);
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> unregisterPushToken(@PathVariable UUID tokenId) {
        log.info("Unregistering push token with ID: {}", tokenId);

        if (!tokenRepository.existsById(tokenId)) {
            return ResponseEntity.notFound().build();
        }

        tokenRepository.deleteById(tokenId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Void> deactivatePushToken(@RequestParam String deviceToken) {
        log.info("Deactivating push token: {}", deviceToken);

        PushNotificationToken token = tokenRepository.findByDeviceToken(deviceToken);

        if (token == null) {
            return ResponseEntity.notFound().build();
        }

        token.setIsActive(false);
        tokenRepository.save(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PushNotificationToken>> getUserPushTokens(@PathVariable UUID userId) {
        log.info("Getting push tokens for user ID: {}", userId);

        List<PushNotificationToken> tokens = tokenRepository.findByUserIdAndIsActive(userId, true);
        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/user/{userId}/platform/{platform}")
    public ResponseEntity<List<PushNotificationToken>> getUserPushTokensByPlatform(
            @PathVariable UUID userId,
            @PathVariable PushNotificationToken.Platform platform) {

        log.info("Getting push tokens for user ID: {} and platform: {}", userId, platform);

        List<PushNotificationToken> tokens = tokenRepository.findByUserIdAndPlatformAndIsActive(userId, platform, true);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/test")
    public ResponseEntity<String> sendTestNotification(
            @RequestParam UUID userId,
            @RequestParam String title,
            @RequestParam String message) {

        log.info("Sending test notification to user ID: {}", userId);

        // Créer une notification de test
        com.smartdelivery.notification.model.Notification testNotification = 
                com.smartdelivery.notification.model.Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .deliveryId(UUID.randomUUID())
                .type(com.smartdelivery.notification.model.Notification.NotificationType.DELIVERY_IN_TRANSIT)
                .channel(com.smartdelivery.notification.model.Notification.NotificationChannel.PUSH)
                .recipient("")
                .subject(title)
                .content(message)
                .status(com.smartdelivery.notification.model.Notification.NotificationStatus.PENDING)
                .build();

        // Envoyer la notification
        pushNotificationService.sendPushNotification(testNotification);

        return ResponseEntity.ok("Test notification sent");
    }
}
