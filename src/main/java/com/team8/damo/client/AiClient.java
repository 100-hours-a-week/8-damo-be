package com.team8.damo.client;

import com.team8.damo.client.request.AiPersonaRequest;
import com.team8.damo.client.request.AiRecommendationRefreshRequest;
import com.team8.damo.client.request.AiRecommendationRequest;
import com.team8.damo.client.request.AiRestaurantConfirmRequest;
import com.team8.damo.client.response.AiPersonaResponse;
import com.team8.damo.client.response.AiRecommendationResponse;
import com.team8.damo.client.response.AiRestaurantConfirmResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/ai/api")
public interface AiClient {

    @GetExchange("/health")
    String health();

    @PostExchange(url = "/v1/recommendations", contentType = "application/json")
    AiRecommendationResponse recommendation(@RequestBody AiRecommendationRequest request);

    @PostExchange(url = "/v1/analyze_refresh", contentType = "application/json")
    AiRecommendationResponse recommendationRefresh(@RequestBody AiRecommendationRefreshRequest request);

    @PostExchange(url = "/v1/update_persona_db", contentType = "application/json")
    AiPersonaResponse updatePersona(@RequestBody AiPersonaRequest request);

    @PostExchange(url = "/v1/restaurant_fix", contentType = "application/json")
    AiRestaurantConfirmResponse confirmRestaurant(@RequestBody AiRestaurantConfirmRequest request);
}
