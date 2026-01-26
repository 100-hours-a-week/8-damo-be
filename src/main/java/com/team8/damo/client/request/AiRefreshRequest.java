package com.team8.damo.client.request;

import java.util.List;

public record AiRefreshRequest(
    Long diningId,
    List<RestaurantVoteResult> voteResultList
) {
}
