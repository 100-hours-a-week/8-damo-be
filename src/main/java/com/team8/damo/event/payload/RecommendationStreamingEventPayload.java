package com.team8.damo.event.payload;

import lombok.Builder;

@Builder
public record RecommendationStreamingEventPayload(
    Long diningId,
    Long userId,
    String content
) implements EventPayload {
}
