package com.team8.damo.service.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RecommendationStreamingResponse(
    Long eventId,
    Long userId,
    String nickname,
    String content,
    LocalDateTime createdAt
) {
}
