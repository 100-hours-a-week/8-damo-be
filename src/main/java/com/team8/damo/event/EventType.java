package com.team8.damo.event;

import com.team8.damo.event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    RESTAURANT_RECOMMENDATION(RecommendationEventPayload.class),
    RESTAURANT_RECOMMENDATION_REFRESH(RecommendationRefreshEventPayload.class),
    USER_PERSONA(UserPersonaPayload.class),
    CREATE_CHAT_MESSAGE(CreateChatMessageEventPayload.class),
    UPDATE_UNREAD_COUNT(UpdateUnreadCountEventPayload.class)
    ;

    private final Class<? extends EventPayload> payloadClass;
}
