package com.team8.damo.event.payload;

import com.team8.damo.client.request.DiningData;
import com.team8.damo.client.request.RestaurantVoteResult;
import lombok.Builder;

import java.util.List;

@Builder
public record RestaurantConfirmedEventPayload(
    DiningData diningData,
    String restaurantId,
    List<RestaurantVoteResult> voteResultList
) implements EventPayload {
}
