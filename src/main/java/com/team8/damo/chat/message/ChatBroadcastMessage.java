package com.team8.damo.chat.message;

import com.team8.damo.event.payload.CreateChatMessageEventPayload;

import java.time.LocalDateTime;

public record ChatBroadcastMessage(
    Long messageId,
    Long senderId,
    Long lightningId,
    ChatType chatType,
    String content,
    LocalDateTime createdAt,
    String senderNickname,
    Long unreadCount,
    String clientMessageId
) {
    public static ChatBroadcastMessage from(CreateChatMessageEventPayload payload, Long unreadCount) {
        return new ChatBroadcastMessage(
            payload.messageId(),
            payload.senderId(),
            payload.lightningId(),
            payload.chatType(),
            payload.content(),
            payload.createdAt(),
            payload.senderNickname(),
            unreadCount,
            payload.clientMessageId()
        );
    }
}
