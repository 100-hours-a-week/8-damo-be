package com.team8.damo.service.response;

import com.team8.damo.entity.enumeration.RestaurantVoteStatus;

public record RestaurantVoteResponse(
    Long recommendRestaurantId,
    String restaurantVoteStatus
) {
    public static RestaurantVoteResponse of(Long id, String status) {
        return new RestaurantVoteResponse(id, status);
    }
}
