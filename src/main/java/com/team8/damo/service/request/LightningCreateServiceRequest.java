package com.team8.damo.service.request;

import java.time.LocalDateTime;

public record LightningCreateServiceRequest(
    String restaurantId,
    int maxParticipants,
    String description,
    LocalDateTime lightningDate
) {}
