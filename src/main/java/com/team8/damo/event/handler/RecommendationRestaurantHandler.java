package com.team8.damo.event.handler;

import com.team8.damo.client.AiService;
import com.team8.damo.client.response.AiRecommendRestaurantsResponse;
import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.RecommendationEventPayload;
import com.team8.damo.service.RecommendRestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationRestaurantHandler implements EventHandler<RecommendationEventPayload> {
    private final AiService aiService;
    private final RecommendRestaurantService recommendRestaurantService;

    @Override
    public void handle(Event<RecommendationEventPayload> event) {
        log.info("[RecommendationRestaurantHandler] handle: {}", event.getPayload().dining().getId());
        RecommendationEventPayload payload = event.getPayload();

        try {
            AiRecommendRestaurantsResponse response = aiService.recommendationRestaurant(
                payload.group(), payload.dining(), payload.userIds()
            );

            // 추천 식당 저장 및 회식 상태를 "장소 투표" 상태로 변경
            recommendRestaurantService.updateRecommendRestaurant(
                payload.dining().getId(),
                response.recommendationCount(),
                response.recommendRestaurants()
            );
        } catch (Exception e) {
            log.error("[RecommendationRestaurantHandler] handle error: diningId={}, groupId={}, userIds={}, msg={}",
                payload.dining().getId(),
                payload.group().getId(),
                payload.userIds(),
                e.getMessage(),
                e
            );
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supports(Event<RecommendationEventPayload> event) {
        return EventType.RESTAURANT_RECOMMENDATION == event.getEventType();
    }
}
