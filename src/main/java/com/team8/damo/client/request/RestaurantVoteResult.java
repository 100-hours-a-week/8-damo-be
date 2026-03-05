package com.team8.damo.client.request;

import lombok.Builder;

import java.util.List;

@Builder
public record RestaurantVoteResult(
    String restaurantId,
    int likeCount,
    int dislikeCount,
    List<Long> likedUserIds,
    List<Long> dislikedUserIds
) {
}
