package com.team8.damo.client;

import com.team8.damo.client.request.*;
import com.team8.damo.client.response.AiPersonaResponse;
import com.team8.damo.client.response.AiRecommendRestaurantsResponse;
import com.team8.damo.client.response.AiRecommendationResponse;
import com.team8.damo.client.response.AiRestaurantConfirmResponse;
import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;
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

    public AiRecommendRestaurantsResponse recommendationRestaurant(Group group, Dining dining, List<Long> userIds) {
        DiningData diningData = createDiningData(group, dining);

        AiRecommendationRequest request = new AiRecommendationRequest(diningData, userIds);
        AiRecommendationResponse recommendation = aiClient.recommendation(request);

        List<RecommendRestaurant> recommendRestaurants = createRecommendRestaurantsBy(dining, recommendation);
        return AiRecommendRestaurantsResponse.of(
            recommendation.recommendationCount(),
            recommendRestaurants
        );
    }

    @Transactional
    public List<RecommendRestaurant> recommendationRefreshRestaurant(Group group, Dining dining, List<Long> userIds) {
        DiningData diningData = createDiningData(group, dining);

        List<RecommendRestaurant> restaurants = recommendRestaurantRepository
            .findByDiningIdAndRecommendationCount(dining.getId(), dining.getRecommendationCount());

        List<RestaurantVoteResult> voteResultList = restaurants.stream()
            .map(this::createVoteResult)
            .toList();

        AiRecommendationRefreshRequest refreshRequest = new AiRecommendationRefreshRequest(diningData, userIds, voteResultList);
        AiRecommendationResponse recommendation = aiClient.recommendationRefresh(refreshRequest);

        dining.changeRecommendationCount(recommendation.recommendationCount());

        recommendation.recommendedItems().forEach(
            recommendedItem -> log.info("recommendedItem: {}", recommendedItem)
        );

        List<RecommendRestaurant> recommendRestaurants = createRecommendRestaurantsBy(dining, recommendation);
        return recommendRestaurantRepository.saveAll(recommendRestaurants);
    }

    @Transactional
    public void userPersonaUpdate(
        User user, List<AllergyType> allergies,
        List<FoodType> likeFoods, List<IngredientType> likeIngredients
    ) {
        UserData userData = UserData.of(user, allergies, likeFoods, likeIngredients);
        AiPersonaRequest request = new AiPersonaRequest(userData);
        AiPersonaResponse response = aiClient.updatePersona(request);
        if (!response.success()) {
            // 실패 시 재처리
        }
    }

    @Transactional
    public void sendConfirmRestaurant(
        Group group,
        Dining dining,
        String restaurantId,
        RecommendRestaurant confirmedRestaurant
    ) {
        DiningData diningData = createDiningData(group, dining);

        List<RestaurantVoteResult> voteResultList = List.of(createVoteResult(confirmedRestaurant));

        AiRestaurantConfirmRequest request = new AiRestaurantConfirmRequest(
            diningData,
            restaurantId,
            voteResultList
        );

        AiRestaurantConfirmResponse response = aiClient.confirmRestaurant(request);
        if (!response.success()) {
            log.warn("AI 식당 확정 전송 실패: restaurantId={}", restaurantId);
        }
    }

    private DiningData createDiningData(Group group, Dining dining) {
        return new DiningData(
            dining.getId(),
            group.getId(),
            dining.getDiningDate(),
            dining.getBudget(),
            String.valueOf(group.getLongitude()),
            String.valueOf(group.getLatitude())
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
