package com.team8.damo.client.request;

import java.util.List;

public record AiRecommendationRefreshRequest(
    DiningData diningData,
    List<Long> userIds,
    List<RestaurantVoteResult> voteResultList
) {
}
