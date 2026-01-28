package com.team8.damo.service.response;

import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.entity.Restaurant;

public record DiningConfirmedResponse(
    Long recommendRestaurantsId,
    String restaurantsName,
    String reasoningDescription,
    String phoneNumber,
    String latitude,
    String longitude
) {
    public static DiningConfirmedResponse of(
        RecommendRestaurant recommendRestaurant,
        Restaurant restaurant
    ) {
        return new DiningConfirmedResponse(
            recommendRestaurant.getId(),
            restaurant.getPlaceName(),
            recommendRestaurant.getReasoningDescription(),
            restaurant.getPhone(),
            restaurant.getLatitude(),
            restaurant.getLongitude()
        );
    }
}
