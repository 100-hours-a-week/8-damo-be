package com.team8.damo.client.response;

import com.team8.damo.entity.RecommendRestaurant;

import java.util.List;

public record AiRecommendRestaurantsResponse(
    int recommendationCount,
    List<RecommendRestaurant> recommendRestaurants
) {
    public static AiRecommendRestaurantsResponse of(int recommendationCount, List<RecommendRestaurant> recommendRestaurants) {
        return new AiRecommendRestaurantsResponse(recommendationCount, recommendRestaurants);
    }
}
