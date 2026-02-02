package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.EventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommonEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(EventType eventType, EventPayload payload) {
        Event<EventPayload> event = Event.of(eventType, payload);
        eventPublisher.publishEvent(event);
    }
}
