package com.team8.damo.event.payload;

import com.team8.damo.service.NotificationService;
import lombok.Builder;

import java.util.List;

@Builder
public record NotificationEventPayload(
    List<String> tokens,
    NotificationService.NotificationInfo notificationInfo
) implements EventPayload {
}
