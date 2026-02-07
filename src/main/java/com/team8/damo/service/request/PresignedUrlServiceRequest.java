package com.team8.damo.service.request;

public record PresignedUrlServiceRequest(
    String fileName,
    String contentType,
    String directory
) {}
