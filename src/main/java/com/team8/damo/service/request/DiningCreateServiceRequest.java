package com.team8.damo.service.request;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.Group;
import com.team8.damo.entity.enumeration.DiningStatus;

import java.time.LocalDateTime;

public record DiningCreateServiceRequest(
    LocalDateTime diningDate,
    LocalDateTime voteDueDate,
    Integer budget
) {
    public Dining toEntity(Long id, Group group) {
        return Dining.builder()
            .id(id)
            .group(group)
            .diningDate(diningDate)
            .voteDueDate(voteDueDate)
            .budget(budget)
            .diningStatus(DiningStatus.ATTENDANCE_VOTING)
            .build();
    }
}
