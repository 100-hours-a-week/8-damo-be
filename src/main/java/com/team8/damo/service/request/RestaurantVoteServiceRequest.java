package com.team8.damo.service.request;

import com.team8.damo.entity.enumeration.VoteStatus;

public record RestaurantVoteServiceRequest(VoteStatus voteStatus) {
}
