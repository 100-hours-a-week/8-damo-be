package com.team8.damo.event;

import com.team8.damo.event.payload.EventPayload;
import com.team8.damo.event.payload.RecommendationEventPayload;
import com.team8.damo.event.payload.UserPersonaPayload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    RESTAURANT_RECOMMENDATION(RecommendationEventPayload.class),
    USER_PERSONA(UserPersonaPayload.class);

    private final Class<? extends EventPayload> payloadClass;
}
