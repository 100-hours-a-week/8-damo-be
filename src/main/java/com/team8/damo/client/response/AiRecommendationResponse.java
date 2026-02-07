package com.team8.damo.client.response;

import java.util.List;

public record AiRecommendationResponse(
        int recommendationCount,
        List<RecommendedItem> recommendedItems
) {
}
