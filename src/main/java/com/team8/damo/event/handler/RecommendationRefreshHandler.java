package com.team8.damo.event.handler;

import com.team8.damo.client.AiService;
import com.team8.damo.client.response.AiRecommendRestaurantsResponse;
import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.RecommendationRefreshEventPayload;
import com.team8.damo.service.RecommendRestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationRefreshHandler implements EventHandler<RecommendationRefreshEventPayload> {

    private final AiService aiService;
    private final RecommendRestaurantService recommendRestaurantService;

    @Override
    public void handle(Event<RecommendationRefreshEventPayload> event) {
        log.info("[RecommendationRefreshHandler] handle: {}", event.getPayload().dining().getId());
        RecommendationRefreshEventPayload payload = event.getPayload();

        try {
            AiRecommendRestaurantsResponse response = aiService.recommendationRefreshRestaurant(
                payload.group(), payload.dining(), payload.userIds()
            );

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
    public boolean supports(Event<RecommendationRefreshEventPayload> event) {
        return EventType.RECOMMENDATION_REFRESH_REQUEST == event.getEventType();
    }
}
