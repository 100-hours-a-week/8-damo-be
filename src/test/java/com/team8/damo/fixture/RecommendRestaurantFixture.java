package com.team8.damo.fixture;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.RecommendRestaurant;

public class RecommendRestaurantFixture {

    public static RecommendRestaurant create(Long id, Dining dining) {
        return RecommendRestaurant.builder()
            .id(id)
            .dining(dining)
            .restaurantId("restaurant-123")
            .confirmedStatus(false)
            .likeCount(0)
            .dislikeCount(0)
            .point(85)
            .reasoningDescription("AI가 추천한 식당입니다.")
            .build();
    }

    public static RecommendRestaurant create(Long id, Dining dining, Integer likeCount, Integer dislikeCount) {
        return RecommendRestaurant.builder()
            .id(id)
            .dining(dining)
            .restaurantId("restaurant-123")
            .confirmedStatus(false)
            .likeCount(likeCount)
            .dislikeCount(dislikeCount)
            .point(85)
            .reasoningDescription("AI가 추천한 식당입니다.")
            .build();
    }
}
