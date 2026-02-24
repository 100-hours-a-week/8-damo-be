package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.RecommendationDoneEventPayload;
import com.team8.damo.service.RecommendRestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationDoneEventHandler implements EventHandler<RecommendationDoneEventPayload> {

    private final RecommendRestaurantService recommendRestaurantService;

    @Override
    public void handle(Event<RecommendationDoneEventPayload> event) {
        RecommendationDoneEventPayload payload = event.getPayload();
        recommendRestaurantService.updateRecommendRestaurantV2(
            payload.groupId(),
            payload.recommendationCount(),
            payload.recommendedItems()
        );
    }

    @Override
    public boolean supports(Event<RecommendationDoneEventPayload> event) {
        return EventType.RECOMMENDATION_RESPONSE == event.getEventType();
    }
}
