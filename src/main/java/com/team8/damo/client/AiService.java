package com.team8.damo.client;

import com.team8.damo.client.request.AiRecommendationRequest;
import com.team8.damo.client.request.DiningData;
import com.team8.damo.client.response.AiRecommendationResponse;
import com.team8.damo.entity.Dining;
import com.team8.damo.entity.Group;
import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.repository.RecommendRestaurantRepository;
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

    @Transactional
    public void recommendationRestaurant(Group group, Dining dining, List<Long> userIds) {
        DiningData diningData = new DiningData(dining.getId(), group.getId(), dining.getDiningDate(), dining.getBudget(), String.valueOf(group.getLongitude()), String.valueOf(group.getLatitude()));
        AiRecommendationRequest request = new AiRecommendationRequest(diningData, userIds);
        AiRecommendationResponse recommendation = aiClient.recommendation(request);
        recommendation.recommendedItems().forEach(
            recommendedItem -> log.info("recommendedItem: {}", recommendedItem)
        );

        List<RecommendRestaurant> recommendRestaurants = createRecommendRestaurantsBy(dining, recommendation);
        recommendRestaurantRepository.saveAll(recommendRestaurants);
    }

    private List<RecommendRestaurant> createRecommendRestaurantsBy(Dining dining, AiRecommendationResponse recommendation) {
        return recommendation.recommendedItems().stream()
            .map(recommendedItem -> RecommendRestaurant.builder()
                .id(snowflake.nextId())
                .restaurantId(recommendedItem.restaurantId())
                .dining(dining)
                .point(0)
                .reasoningDescription(recommendedItem.reasoningDescription())
                .build()
            )
            .toList();
    }
}
