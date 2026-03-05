package com.team8.damo.event.payload;

import com.team8.damo.client.request.DiningData;
import com.team8.damo.client.request.RestaurantVoteResult;
import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationRefreshV2EventPayload(
    DiningData diningData,
    List<Long> userIds,
    List<RestaurantVoteResult> voteResultList
) implements EventPayload {
}
