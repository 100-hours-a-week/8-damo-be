package com.team8.damo.client.response;

public record AiLightningResponse(
    String restaurantId,
    String restaurantName,
    String x,
    String y,
    String phoneNumber
) {
}
