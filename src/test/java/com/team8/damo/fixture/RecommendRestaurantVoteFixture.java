package com.team8.damo.fixture;

import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.entity.RecommendRestaurantVote;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.RestaurantVoteStatus;

public class RecommendRestaurantVoteFixture {

    public static RecommendRestaurantVote create(Long id, User user, RecommendRestaurant restaurant, RestaurantVoteStatus status) {
        return RecommendRestaurantVote.builder()
            .id(id)
            .user(user)
            .recommendRestaurant(restaurant)
            .status(status)
            .build();
    }
}
