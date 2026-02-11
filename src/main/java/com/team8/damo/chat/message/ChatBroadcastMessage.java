package com.team8.damo.chat.message;

import java.time.LocalDateTime;

public record ChatBroadcastMessage(
    Long senderId,
    Long lightningId,
    ChatType chatType,
    String content,
    LocalDateTime createdAt
) { }
