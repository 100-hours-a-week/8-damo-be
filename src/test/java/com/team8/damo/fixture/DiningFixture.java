package com.team8.damo.fixture;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.Group;
import com.team8.damo.entity.enumeration.DiningStatus;

import java.time.LocalDateTime;

public class DiningFixture {

    public static Dining create(Long id, Group group) {
        return Dining.builder()
            .id(id)
            .group(group)
            .diningDate(LocalDateTime.of(2025, 12, 25, 18, 0))
            .voteDueDate(LocalDateTime.of(2025, 12, 20, 23, 59))
            .budget(30000)
            .diningStatus(DiningStatus.ATTENDANCE_VOTING)
            .build();
    }

    public static Dining create(Long id, Group group, DiningStatus status) {
        return Dining.builder()
            .id(id)
            .group(group)
            .diningDate(LocalDateTime.of(2025, 12, 25, 18, 0))
            .voteDueDate(LocalDateTime.of(2025, 12, 20, 23, 59))
            .budget(30000)
            .diningStatus(status)
            .build();
    }

    public static Dining create(
        Long id,
        Group group,
        LocalDateTime diningDate,
        LocalDateTime voteDueDate,
        Integer budget
    ) {
        return Dining.builder()
            .id(id)
            .group(group)
            .diningDate(diningDate)
            .voteDueDate(voteDueDate)
            .budget(budget)
            .diningStatus(DiningStatus.ATTENDANCE_VOTING)
            .build();
    }

    public static Dining createWithRecommendationCount(
        Long id,
        Group group,
        DiningStatus status,
        Integer recommendationCount
    ) {
        Dining dining = Dining.builder()
            .id(id)
            .group(group)
            .diningDate(LocalDateTime.of(2025, 12, 25, 18, 0))
            .voteDueDate(LocalDateTime.of(2025, 12, 20, 23, 59))
            .budget(30000)
            .diningStatus(status)
            .build();
        dining.changeRecommendationCount(recommendationCount);
        return dining;
    }
}
