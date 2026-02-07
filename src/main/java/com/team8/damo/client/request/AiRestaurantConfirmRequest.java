package com.team8.damo.client.request;

import java.util.List;

public record AiRestaurantConfirmRequest(
    DiningData diningData,
    String restaurantId,
    List<RestaurantVoteResult> voteResultList
) {
}
