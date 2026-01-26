package com.team8.damo.client.request;

import java.util.List;

public record RestaurantVoteResult(
    String restaurantId,
    int likeCount,
    int dislikeCount,
    List<Long> likeUserIds,
    List<Long> dislikeUserIds
) {
}
