package com.team8.damo.event.payload;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.Group;
import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationRefreshEventPayload(
    Group group,
    Dining dining,
    List<Long> userIds
) implements EventPayload {
}
