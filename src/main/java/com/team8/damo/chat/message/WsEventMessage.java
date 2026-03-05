package com.team8.damo.chat.message;

public record WsEventMessage(
    WsEventType type,
    Long lightningId,
    Object payload
) {
    public static WsEventMessage createChatMessage(Long lightningId, Object payload) {
        return new WsEventMessage(
            WsEventType.CHAT_MESSAGE,
            lightningId,
            payload
        );
    }

    public static WsEventMessage createUnreadUpdate(Long lightningId, Object payload) {
        return new WsEventMessage(
            WsEventType.UNREAD_UPDATE,
            lightningId,
            payload
        );
    }
}
