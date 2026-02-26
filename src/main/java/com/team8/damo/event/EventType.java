package com.team8.damo.event;

import com.team8.damo.event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    RECOMMENDATION_REQUEST(RecommendationEventPayload.class, Topic.RECOMMENDATION_REQUEST),
    RECOMMENDATION_RESPONSE(RecommendationDoneEventPayload.class, Topic.RECOMMENDATION_RESPONSE),
    RECOMMENDATION_REFRESH_REQUEST(RecommendationRefreshEventPayload.class, Topic.RECOMMENDATION_REFRESH_REQUEST),
    USER_PERSONA_UPDATE(UserPersonaEventPayload.class, Topic.USER_PERSONA_UPDATE),
    RESTAURANT_CONFIRMED(RestaurantConfirmedEventPayload.class, Topic.RESTAURANT_CONFIRMED),
    CONSENSUS_DIALOGUE(RecommendationStreamingEventPayload.class, Topic.RECOMMENDATION_STREAMING),
    USER_PERSONA(UserPersonaPayload.class, ""),
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
        // produce topic
        public static final String RECOMMENDATION_REQUEST = "recommendation-request";
        public static final String RECOMMENDATION_REFRESH_REQUEST = "recommendation-refresh-request";
        public static final String USER_PERSONA_UPDATE = "user-persona-update";
        public static final String RESTAURANT_CONFIRMED = "restaurant-confirmed";
        public static final String RECEIPT_OCR_REQUEST = "receipt-ocr-request";

        // consume topic
        public static final String RECOMMENDATION_RESPONSE = "recommendation-response";
        public static final String RECOMMENDATION_STREAMING = "recommendation-streaming";
        public static final String RECEIPT_OCR_RESPONSE = "receipt-ocr-response";
    }
}
