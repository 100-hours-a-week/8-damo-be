package com.team8.damo.chat.message;

public record UnreadCountMessage(
    Long userId,
    Long start
) {
}
