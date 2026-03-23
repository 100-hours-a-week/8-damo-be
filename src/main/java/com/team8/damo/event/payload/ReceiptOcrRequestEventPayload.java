package com.team8.damo.event.payload;

import lombok.Builder;

@Builder
public record ReceiptOcrRequestEventPayload(
    Long diningId,
    String presignedUrl,
    String restaurantName
) implements EventPayload {
}
