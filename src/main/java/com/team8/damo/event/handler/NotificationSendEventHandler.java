package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.NotificationEventPayload;
import com.team8.damo.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSendEventHandler implements EventHandler<NotificationEventPayload> {

    private final FcmService fcmService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void handle(Event<NotificationEventPayload> event) {
        String dedupKey = "notif:sent:" + event.getEventId();

        Boolean isNew = redisTemplate.opsForValue()
            .setIfAbsent(dedupKey, "1", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNew)) {
            log.info("Duplicate notification event skipped: {}", event.getEventId());
            return;
        }

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
