package com.team8.damo.event.payload;

public record ReceiptOcrResponseEventPayload(
    Long diningId,
    boolean success
) implements EventPayload {
}
