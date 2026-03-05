package com.team8.damo.service.response;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.enumeration.LightningStatus;

import java.time.LocalDateTime;

public record AvailableLightningResponse(
    Long lightningId,
    String restaurantName,
    String description,
    int maxParticipants,
    int participantsCount,
    LightningStatus lightningStatus,
    LocalDateTime lightningData
) {

    public static AvailableLightningResponse of(
        Lightning lightning,
        String restaurantName,
        int participantsCount
    ) {
        return new AvailableLightningResponse(
            lightning.getId(),
            restaurantName,
            lightning.getDescription(),
            lightning.getMaxParticipants(),
            participantsCount,
            lightning.getLightningStatus(),
            lightning.getLightningDate()
        );
    }
}
