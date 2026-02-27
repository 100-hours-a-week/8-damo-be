package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.RecommendationStreamingEventPayload;
import com.team8.damo.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationStreamingHandler implements EventHandler<RecommendationStreamingEventPayload> {

    private final SseEmitterService sseEmitterService;

    @Override
    public void handle(Event<RecommendationStreamingEventPayload> event) {
        RecommendationStreamingEventPayload payload = event.getPayload();
        sseEmitterService.streamingBroadcast(payload.diningId(), payload);
    }

    @Override
    public boolean supports(Event<RecommendationStreamingEventPayload> event) {
        return event.getEventType() == EventType.CONSENSUS_DIALOGUE;
    }
}
