package com.team8.damo.service.response;

public record PresignedUrlResponse(
    String presignedUrl,
    String objectKey,
    int expiresIn
) {}
