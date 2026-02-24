package com.team8.damo.event.payload;

import java.util.List;

public record RecommendationDoneEventPayload(
    Long groupId,
    int recommendationCount,
    List<RecommendedItem> recommendedItems
) implements EventPayload {
    public record RecommendedItem(
        String restaurantId,
        String reasoningDescription
    ) {}
}
