package com.team8.damo.event.handler;

import com.team8.damo.client.AiService;
import com.team8.damo.client.response.AiRecommendRestaurantsResponse;
import com.team8.damo.event.RestaurantRecommendationEvent;
import com.team8.damo.event.UserPersonaEvent;
import com.team8.damo.service.RecommendRestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventHandler {

    private final AiService aiService;
    private final RecommendRestaurantService recommendRestaurantService;

    @Async("eventRelayExecutor")
    @Retryable(
        delay = 200L,
        multiplier = 1.5
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRestaurantRecommendationEvent(RestaurantRecommendationEvent event) {
        AiRecommendRestaurantsResponse response = aiService.recommendationRestaurant(
            event.group(), event.dining(), event.userIds()
        );

        // 추천 식당 저장 및 회식 상태를 "장소 투표" 상태로 변경
        recommendRestaurantService.updateRecommendRestaurant(
            event.dining().getId(),
            response.recommendationCount(),
            response.recommendRestaurants()
        );
    }

    @Async("eventRelayExecutor")
    @Retryable(
        delay = 200L,
        multiplier = 1.5
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserPersonaEvent(UserPersonaEvent event) {
        aiService.userPersonaUpdate(
            event.user(),
            event.allergies(),
            event.likeFoods(),
            event.likeIngredients()
        );
    }
}
