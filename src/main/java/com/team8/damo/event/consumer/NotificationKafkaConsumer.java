package com.team8.damo.event.consumer;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.EventPayload;
import com.team8.damo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = {
            EventType.Topic.RECOMMENDATION_REQUEST,
            EventType.Topic.RECOMMENDATION_RESPONSE,
            EventType.Topic.RESTAURANT_CONFIRMED
        },
        groupId = "${spring.kafka.consumer.group-id}-notification"
    )
    public void consume(String message, Acknowledgment ack) {
        Event<EventPayload> event = Event.fromJson(message);
        log.info("[NotificationKafkaConsumer] eventType={}", event.getEventType());
        notificationService.sendDiningNotificationV2(event);
        ack.acknowledge();
    }
}
