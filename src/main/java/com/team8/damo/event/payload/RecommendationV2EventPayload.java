package com.team8.damo.event.payload;

import com.team8.damo.client.request.DiningData;
import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationV2EventPayload(
    DiningData diningData,
    List<Long> userIds
) implements EventPayload {
}
