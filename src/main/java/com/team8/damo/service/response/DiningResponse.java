package com.team8.damo.service.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team8.damo.entity.Dining;
import com.team8.damo.entity.enumeration.DiningStatus;

import java.time.LocalDateTime;

public record DiningResponse(
    Long diningId,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    LocalDateTime diningDate,
    DiningStatus status,
    int diningParticipantsCount
) {
    public static DiningResponse of(Dining dining, int participantsCount) {
        return new DiningResponse(
            dining.getId(),
            dining.getDiningDate(),
            dining.getDiningStatus(),
            participantsCount
        );
    }
}
