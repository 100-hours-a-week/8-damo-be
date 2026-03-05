package com.team8.damo.service.request;

public record PushNotificationUpdateServiceRequest(
    String fcmToken,
    boolean isPushNotificationAllowed
) {}
