package com.team8.damo.fixture;

import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.entity.RecommendRestaurantVote;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.VoteStatus;

public class RecommendRestaurantVoteFixture {

    public static RecommendRestaurantVote create(Long id, User user, RecommendRestaurant restaurant, VoteStatus status) {
        return RecommendRestaurantVote.builder()
            .id(id)
            .user(user)
            .recommendRestaurant(restaurant)
            .status(status)
            .build();
    }
}
