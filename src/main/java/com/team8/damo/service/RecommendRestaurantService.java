package com.team8.damo.service;

import com.team8.damo.client.response.AiRecommendationResponse;
import com.team8.damo.entity.Dining;
import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.event.payload.RecommendationDoneEventPayload;
import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.repository.DiningRepository;
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
public class RecommendRestaurantService {

    private final Snowflake snowflake;
    private final DiningRepository diningRepository;
    private final RecommendRestaurantRepository recommendRestaurantRepository;

    @Transactional
    public void updateRecommendRestaurant(Long diningId, int recommendationCount, List<RecommendRestaurant> recommendRestaurants) {
        recommendRestaurantRepository.saveAll(recommendRestaurants);

        Dining dining = diningRepository.findById(diningId)
            .orElseThrow(() -> new CustomException(ErrorCode.DINING_NOT_FOUND));
        dining.startRestaurantVoting();
        dining.changeRecommendationCount(recommendationCount);

        log.info("recommendationCount: {}", recommendationCount);
        recommendRestaurants.forEach(recommendRestaurant -> {
            log.info("recommendRestaurant: {} {}",  recommendRestaurant.getRestaurantId(), recommendRestaurant.getReasoningDescription());
        });
    }

    @Transactional
    public void updateRecommendRestaurantV2(Long diningId, int recommendationCount, List<RecommendationDoneEventPayload.RecommendedItem> recommendedItems) {
        Dining dining = diningRepository.findById(diningId)
            .orElseThrow(() -> new CustomException(ErrorCode.DINING_NOT_FOUND));

        List<RecommendRestaurant> recommendRestaurants = createRecommendRestaurantsBy(dining, recommendationCount, recommendedItems);
        recommendRestaurantRepository.saveAll(recommendRestaurants);

        dining.startRestaurantVoting();
        dining.changeRecommendationCount(recommendationCount);
    }

    private List<RecommendRestaurant> createRecommendRestaurantsBy(Dining dining, int recommendationCount, List<RecommendationDoneEventPayload.RecommendedItem> recommendedItems) {
        return recommendedItems.stream()
            .map(recommendedItem -> RecommendRestaurant.builder()
                .id(snowflake.nextId())
                .restaurantId(recommendedItem.restaurantId())
                .dining(dining)
                .point(0)
                .reasoningDescription(recommendedItem.reasoningDescription())
                .recommendationCount(recommendationCount)
                .build()
            )
            .toList();
    }
}
