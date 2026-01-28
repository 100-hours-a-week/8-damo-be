package com.team8.damo.fixture;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.RecommendRestaurant;

public class RecommendRestaurantFixture {

    public static RecommendRestaurant create(Long id, Dining dining) {
        return RecommendRestaurant.builder()
            .id(id)
            .dining(dining)
            .restaurantId("6976b54010e1fa815903d4ce")
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
            .restaurantId("6976b54010e1fa815903d4ce")
            .confirmedStatus(false)
            .likeCount(likeCount)
            .dislikeCount(dislikeCount)
            .point(85)
            .reasoningDescription("AI가 추천한 식당입니다.")
            .build();
    }

    public static RecommendRestaurant create(Long id, Dining dining, String restaurantId, Integer recommendationCount) {
        return RecommendRestaurant.builder()
            .id(id)
            .dining(dining)
            .restaurantId(restaurantId)
            .confirmedStatus(false)
            .likeCount(0)
            .dislikeCount(0)
            .point(85)
            .reasoningDescription("AI가 추천한 식당입니다.")
            .recommendationCount(recommendationCount)
            .build();
    }

    public static RecommendRestaurant create(
        Long id,
        Dining dining,
        String restaurantId,
        Integer recommendationCount,
        Integer likeCount,
        Integer dislikeCount
    ) {
        return RecommendRestaurant.builder()
            .id(id)
            .dining(dining)
            .restaurantId(restaurantId)
            .confirmedStatus(false)
            .likeCount(likeCount)
            .dislikeCount(dislikeCount)
            .point(85)
            .reasoningDescription("AI가 추천한 식당입니다.")
            .recommendationCount(recommendationCount)
            .build();
    }

    public static RecommendRestaurant createConfirmed(
        Long id,
        Dining dining,
        String restaurantId,
        Integer recommendationCount
    ) {
        return RecommendRestaurant.builder()
            .id(id)
            .dining(dining)
            .restaurantId(restaurantId)
            .confirmedStatus(true)
            .likeCount(10)
            .dislikeCount(2)
            .point(90)
            .reasoningDescription("AI가 추천한 최고의 식당입니다.")
            .recommendationCount(recommendationCount)
            .build();
    }
}
