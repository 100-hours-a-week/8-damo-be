package com.team8.damo.service.response;

import com.team8.damo.entity.enumeration.RestaurantVoteStatus;

public record RestaurantVoteResponse(
    Long recommendRestaurantId,
    RestaurantVoteStatus restaurantVoteStatus
) {
    public static RestaurantVoteResponse of(Long id, RestaurantVoteStatus status) {
        return new RestaurantVoteResponse(id, status);
    }
}
