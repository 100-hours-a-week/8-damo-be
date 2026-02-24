package com.team8.damo.event;

import com.team8.damo.event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    RECOMMENDATION_REQUEST(RecommendationEventPayload.class, Topic.RECOMMENDATION_REQUEST),
    RECOMMENDATION_REFRESH_REQUEST(RecommendationRefreshEventPayload.class, ""),
    USER_PERSONA(UserPersonaPayload.class, Topic.USER_PERSONA),
    CREATE_CHAT_MESSAGE(CreateChatMessageEventPayload.class, ""),
    UPDATE_UNREAD_COUNT(UpdateUnreadCountEventPayload.class, "")
    ;

    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static EventType from(String type) {
        try {
            return valueOf(type);
        } catch (Exception e) {
            return null;
        }
    }

    public static class Topic {
        public static final String RECOMMENDATION_REQUEST = "recommendation-request";
        public static final String RECOMMENDATION_RESPONSE = "recommendation-response";
        public static final String RECOMMENDATION_REFRESH_REQUEST = "recommendation-refresh-request";
        public static final String USER_PERSONA = "user-persona";
    }
}
