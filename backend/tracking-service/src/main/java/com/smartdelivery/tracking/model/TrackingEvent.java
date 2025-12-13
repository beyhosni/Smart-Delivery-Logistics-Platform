package com.smartdelivery.tracking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "tracking_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEvent {

    @Id
    private String id;

    @Field("deliveryId")
    private UUID deliveryId;

    @Field("eventType")
    private TrackingEventType eventType;

    @Field("timestamp")
    private LocalDateTime timestamp;

    @Field("location")
    private GeoLocation location;

    @Field("description")
    private String description;

    @Field("courierId")
    private UUID courierId;

    @Field("recipientName")
    private String recipientName;

    @Field("signature")
    private String signature;

    @Field("photo")
    private String photo;

    public enum TrackingEventType {
        PICKED_UP, IN_TRANSIT, DELIVERED, FAILED_ATTEMPT, DELAYED
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeoLocation {
        private String type;
        private Double[] coordinates; // [longitude, latitude] selon GeoJSON
    }
}
