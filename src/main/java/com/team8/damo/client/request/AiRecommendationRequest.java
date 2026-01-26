package com.team8.damo.client.request;

import java.util.List;

public record AiRecommendationRequest(
        DiningData diningData,
        List<Long> userIds
) {
}
