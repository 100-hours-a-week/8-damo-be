package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.NotificationEventPayload;
import com.team8.damo.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendEventHandler implements EventHandler<NotificationEventPayload> {

    private final FcmService fcmService;

    @Override
    public void handle(Event<NotificationEventPayload> event) {
        NotificationEventPayload payload = event.getPayload();
        fcmService.sendMulticast(
            payload.tokens(),
            payload.notificationInfo().title(),
            payload.notificationInfo().body(),
            Map.of(
                "groupId", String.valueOf(payload.notificationInfo().groupId()),
                "diningId", String.valueOf(payload.notificationInfo().diningId())
            )
        );
    }

    @Override
    public boolean supports(Event<NotificationEventPayload> event) {
        return event.getEventType() == EventType.NOTIFICATION_SEND;
    }
}
