package com.team8.damo.service.response;

import com.team8.damo.entity.enumeration.VoteStatus;

public record RestaurantVoteResponse(
    Long recommendRestaurantId,
    VoteStatus voteStatus
) {
    public static RestaurantVoteResponse of(Long id, VoteStatus status) {
        return new RestaurantVoteResponse(id, status);
    }
}
