package com.team8.damo.service.response;

import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.enumeration.RestaurantVoteStatus;

public record RestaurantVoteDetailResponse(
    Long recommendRestaurantsId,
    String restaurantsName,
    String reasoningDescription,
    String restaurantVoteStatus,
    String phoneNumber,
    String latitude,
    String longitude,
    int likeCount,
    int dislikeCount
) {
    public static RestaurantVoteDetailResponse of(
        RecommendRestaurant recommendRestaurant,
        Restaurant restaurant,
        RestaurantVoteStatus userVoteStatus
    ) {
        return new RestaurantVoteDetailResponse(
            recommendRestaurant.getId(),
            restaurant.getPlaceName(),
            recommendRestaurant.getReasoningDescription(),
            userVoteStatus != null ? userVoteStatus.name() : "NONE",
            restaurant.getPhone(),
            restaurant.getLatitude(),
            restaurant.getLongitude(),
            recommendRestaurant.getLikeCount(),
            recommendRestaurant.getDislikeCount()
        );
    }
}
