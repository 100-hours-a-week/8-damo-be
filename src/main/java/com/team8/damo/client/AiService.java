package com.team8.damo.client;

import com.team8.damo.client.request.AiRecommendationRefreshRequest;
import com.team8.damo.client.request.AiRecommendationRequest;
import com.team8.damo.client.request.DiningData;
import com.team8.damo.client.request.RestaurantVoteResult;
import com.team8.damo.client.response.AiRecommendationResponse;
import com.team8.damo.entity.Dining;
import com.team8.damo.entity.Group;
import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.entity.RecommendRestaurantVote;
import com.team8.damo.entity.enumeration.RestaurantVoteStatus;
import com.team8.damo.repository.RecommendRestaurantRepository;
import com.team8.damo.repository.RecommendRestaurantVoteRepository;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {
    private final Snowflake snowflake;
    private final AiClient aiClient;
    private final RecommendRestaurantRepository recommendRestaurantRepository;
    private final RecommendRestaurantVoteRepository recommendRestaurantVoteRepository;

    @Transactional
    public void recommendationRestaurant(Group group, Dining dining, List<Long> userIds) {
        DiningData diningData = new DiningData(dining.getId(), group.getId(), dining.getDiningDate(), dining.getBudget(), String.valueOf(group.getLongitude()), String.valueOf(group.getLatitude()));

        AiRecommendationRequest request = new AiRecommendationRequest(diningData, userIds);
        AiRecommendationResponse recommendation = aiClient.recommendation(request);

        List<RecommendRestaurant> recommendRestaurants = createRecommendRestaurantsBy(dining, recommendation);
        recommendRestaurantRepository.saveAll(recommendRestaurants);

        dining.changeRecommendationCount(recommendation.recommendationCount());

        recommendation.recommendedItems().forEach(
            recommendedItem -> log.info("recommendedItem: {}", recommendedItem)
        );
    }

    @Transactional
    public void recommendationRefreshRestaurant(Group group, Dining dining, List<Long> userIds) {
        DiningData diningData = new DiningData(dining.getId(), group.getId(), dining.getDiningDate(), dining.getBudget(), String.valueOf(group.getLongitude()), String.valueOf(group.getLatitude()));

        List<RecommendRestaurant> restaurants = recommendRestaurantRepository
            .findByDiningIdAndRecommendationCount(dining.getId(), dining.getRecommendationCount());

        List<RestaurantVoteResult> voteResultList = restaurants.stream()
            .map(this::createVoteResult)
            .toList();

        AiRecommendationRefreshRequest refreshRequest = new AiRecommendationRefreshRequest(diningData, userIds, voteResultList);
        AiRecommendationResponse recommendation = aiClient.recommendationRefresh(refreshRequest);

        List<RecommendRestaurant> recommendRestaurants = createRecommendRestaurantsBy(dining, recommendation);
        recommendRestaurantRepository.saveAll(recommendRestaurants);

        dining.changeRecommendationCount(recommendation.recommendationCount());

        recommendation.recommendedItems().forEach(
            recommendedItem -> log.info("recommendedItem: {}", recommendedItem)
        );
    }

    private RestaurantVoteResult createVoteResult(RecommendRestaurant restaurant) {
        List<RecommendRestaurantVote> votes = recommendRestaurantVoteRepository
            .findByRecommendRestaurantId(restaurant.getId());

        List<Long> likedUserIds = votes.stream()
            .filter(v -> v.getStatus() == RestaurantVoteStatus.LIKE)
            .map(v -> v.getUser().getId())
            .toList();

        List<Long> dislikedUserIds = votes.stream()
            .filter(v -> v.getStatus() == RestaurantVoteStatus.DISLIKE)
            .map(v -> v.getUser().getId())
            .toList();

        return new RestaurantVoteResult(
            restaurant.getRestaurantId(),
            restaurant.getLikeCount(),
            restaurant.getDislikeCount(),
            likedUserIds,
            dislikedUserIds
        );
    }

    private List<RecommendRestaurant> createRecommendRestaurantsBy(Dining dining, AiRecommendationResponse recommendation) {
        return recommendation.recommendedItems().stream()
            .map(recommendedItem -> RecommendRestaurant.builder()
                .id(snowflake.nextId())
                .restaurantId(recommendedItem.restaurantId())
                .dining(dining)
                .point(0)
                .reasoningDescription(recommendedItem.reasoningDescription())
                .recommendationCount(recommendation.recommendationCount())
                .build()
            )
            .toList();
    }
}
