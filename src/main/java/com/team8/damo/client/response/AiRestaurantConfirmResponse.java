package com.team8.damo.client.response;

public record AiRestaurantConfirmResponse(
    boolean success,
    String restaurantId
) {
}
