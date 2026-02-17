package com.team8.damo.event.payload;

import lombok.Builder;

@Builder
public record UpdateUnreadCountEventPayload(
    Long userId,
    Long lightningId,
    Long startChatMessageId,
    Long endChatMessageId
) implements EventPayload {
}
