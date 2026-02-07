package com.team8.damo.service.response;

public record JwtTokenResponse(
    String accessToken,
    String refreshToken
) {
}
