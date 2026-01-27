package com.team8.damo.entity.enumeration;

import com.team8.damo.repository.RecommendRestaurantRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RestaurantVoteStatus implements RestaurantVoteCounter {
    LIKE("추천") {
        @Override
        public void increaseCount(Long recommendRestaurantId, RecommendRestaurantRepository repository) {
            repository.increaseLikeCount(recommendRestaurantId);
        }

        @Override
        public void decreaseCount(Long recommendRestaurantId, RecommendRestaurantRepository repository) {
            repository.decreaseLikeCount(recommendRestaurantId);
        }
    },
    DISLIKE("비추천") {
        @Override
        public void increaseCount(Long recommendRestaurantId, RecommendRestaurantRepository repository) {
            repository.increaseDislikeCount(recommendRestaurantId);
        }

        @Override
        public void decreaseCount(Long recommendRestaurantId, RecommendRestaurantRepository repository) {
            repository.decreaseDislikeCount(recommendRestaurantId);
        }
    };

    private final String description;
}
