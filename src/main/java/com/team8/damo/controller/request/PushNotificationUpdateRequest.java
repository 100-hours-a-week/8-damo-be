package com.team8.damo.controller.request;

import com.team8.damo.service.request.PushNotificationUpdateServiceRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PushNotificationUpdateRequest {

    private String fcmToken;

    @NotNull(message = "알림 허용 여부는 필수입니다.")
    private Boolean isPushNotificationAllowed;

    public PushNotificationUpdateServiceRequest toServiceRequest() {
        return new PushNotificationUpdateServiceRequest(fcmToken, isPushNotificationAllowed);
    }
}
