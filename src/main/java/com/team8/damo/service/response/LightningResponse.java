package com.team8.damo.service.response;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.enumeration.GatheringRole;
import com.team8.damo.entity.enumeration.LightningStatus;

import java.time.LocalDateTime;

public record LightningResponse(
    Long lightningId,
    String restaurantName,
    String description,
    int maxParticipants,
    int participantsCount,
    LightningStatus lightningStatus,
    GatheringRole myRole,
    LocalDateTime lightningDate
) {

    public static LightningResponse of(
        Lightning lightning,
        String restaurantName,
        int participantsCount,
        GatheringRole myRole
    ) {
        return new LightningResponse(
            lightning.getId(),
            restaurantName,
            lightning.getDescription(),
            lightning.getMaxParticipants(),
            participantsCount,
            lightning.getLightningStatus(),
            myRole,
            lightning.getLightningDate()
        );
    }
}
