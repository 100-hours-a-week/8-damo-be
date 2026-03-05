package com.team8.damo.event.payload;

import java.util.List;

public record RecommendationDoneEventPayload(
    Long diningId,
    int recommendationCount,
    List<RecommendedItem> recommendedItems
) implements EventPayload {
    public record RecommendedItem(
        String restaurantId,
        String reasoningDescription
    ) {}
}
