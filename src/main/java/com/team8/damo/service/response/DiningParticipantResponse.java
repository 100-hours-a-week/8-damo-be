package com.team8.damo.service.response;

import com.team8.damo.entity.DiningParticipant;

public record DiningParticipantResponse(
    Long userId,
    String nickname,
    String imagePath
) {
    public static DiningParticipantResponse from(DiningParticipant participant) {
        return new DiningParticipantResponse(
            participant.getUser().getId(),
            participant.getUser().getNickname(),
            participant.getUser().getImagePath()
        );
    }
}
