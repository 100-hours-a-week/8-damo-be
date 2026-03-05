package com.team8.damo.controller.request;

import com.team8.damo.chat.message.ChatType;

public record ChatMessageRequest(
    ChatType chatType,
    String content
) {
}
