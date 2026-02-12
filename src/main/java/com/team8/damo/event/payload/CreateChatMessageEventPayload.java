package com.team8.damo.event.payload;

import com.team8.damo.chat.message.ChatType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateChatMessageEventPayload(
    Long messageId,
    Long senderId,
    Long lightningId,
    ChatType chatType,
    String content,
    LocalDateTime createdAt,
    String senderNickname
) implements EventPayload {
}
