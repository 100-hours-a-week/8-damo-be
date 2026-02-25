package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.KafkaEvent;
import com.team8.damo.event.payload.EventPayload;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommonEventPublisher {

    private final Snowflake snowflake;
    private final ApplicationEventPublisher eventPublisher;

    public void publish(EventType eventType, EventPayload payload) {
        Event<EventPayload> event = Event.of(snowflake.nextId(), eventType, payload);
        eventPublisher.publishEvent(event);
    }

    public void publishKafka(EventType eventType, EventPayload payload) {
        String eventPayload = Event.of(snowflake.nextId(), eventType, payload).toJson();
        KafkaEvent kafkaEvent = new KafkaEvent(eventType.getTopic(), eventPayload);
        eventPublisher.publishEvent(kafkaEvent);
    }
}
