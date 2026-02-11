package com.team8.damo.service.response;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.enumeration.GatheringRole;
import com.team8.damo.entity.enumeration.LightningStatus;

import java.time.LocalDateTime;
import java.util.List;

public record LightningDetailResponse(
    Long lightningId,
    String restaurantName,
    String longitude,
    String latitude,
    String description,
    LocalDateTime lightningDate,
    int maxParticipants,
    int participantsCount,
    LightningStatus lightningStatus,
    List<ParticipantResponse> participants
) {

    public record ParticipantResponse(
        Long userId,
        String nickname,
        GatheringRole role
    ) {

        public static ParticipantResponse from(LightningParticipant participant) {
            return new ParticipantResponse(
                participant.getUser().getId(),
                participant.getUser().getNickname(),
                participant.getRole()
            );
        }
    }

    public static LightningDetailResponse of(
        Lightning lightning,
        Restaurant restaurant,
        List<LightningParticipant> participants
    ) {
        String restaurantName = restaurant != null ? restaurant.getPlaceName() : "";
        String longitude = restaurant != null ? restaurant.getLongitude() : "";
        String latitude = restaurant != null ? restaurant.getLatitude() : "";

        List<ParticipantResponse> participantResponses = participants.stream()
            .map(ParticipantResponse::from)
            .toList();

        return new LightningDetailResponse(
            lightning.getId(),
            restaurantName,
            longitude,
            latitude,
            lightning.getDescription(),
            lightning.getLightningDate(),
            lightning.getMaxParticipants(),
            participants.size(),
            lightning.getLightningStatus(),
            participantResponses
        );
    }
}
