package com.team8.damo.service.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team8.damo.entity.Dining;
import com.team8.damo.entity.enumeration.DiningStatus;

import java.time.LocalDateTime;
import java.util.List;

public record DiningDetailResponse(
    boolean isGroupLeader,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    LocalDateTime diningDate,
    DiningStatus diningStatus,
    List<DiningParticipantResponse> diningParticipants
) {
    public static DiningDetailResponse of(
        boolean isGroupLeader,
        Dining dining,
        List<DiningParticipantResponse> participants
    ) {
        return new DiningDetailResponse(
            isGroupLeader,
            dining.getDiningDate(),
            dining.getDiningStatus(),
            participants
        );
    }
}
