package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.RecommendationDoneEventPayload;
import com.team8.damo.service.RecommendRestaurantService;
import com.team8.damo.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationDoneEventHandler implements EventHandler<RecommendationDoneEventPayload> {

    private final SseEmitterService sseEmitterService;
    private final RecommendRestaurantService recommendRestaurantService;

    @Override
    public void handle(Event<RecommendationDoneEventPayload> event) {
        RecommendationDoneEventPayload payload = event.getPayload();
        recommendRestaurantService.updateRecommendRestaurantV2(
            payload.diningId(),
            payload.recommendationCount(),
            payload.recommendedItems()
        );
        sseEmitterService.completeAll(payload.diningId());
    }

    @Override
    public boolean supports(Event<RecommendationDoneEventPayload> event) {
        return EventType.RECOMMENDATION_RESPONSE == event.getEventType();
    }
}
