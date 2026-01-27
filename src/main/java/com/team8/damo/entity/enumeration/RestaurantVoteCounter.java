package com.team8.damo.entity.enumeration;

import com.team8.damo.repository.RecommendRestaurantRepository;

public interface RestaurantVoteCounter {
    void increaseCount(Long recommendRestaurantId, RecommendRestaurantRepository repository);
    void decreaseCount(Long recommendRestaurantId, RecommendRestaurantRepository repository);
}
