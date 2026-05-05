package com.team8.damo.event.consumer;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.handler.EventHandler;
import com.team8.damo.event.payload.EventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumer {

    private final List<EventHandler> eventHandlers;

    @KafkaListener(topics = {
        EventType.Topic.RECOMMENDATION_RESPONSE
    })
    public void consume1(String message, Acknowledgment ack) {
        handleEvent(message);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = EventType.Topic.RECOMMENDATION_STREAMING
    )
    public void consume2(String message, Acknowledgment ack) {
        handleEvent(message);
        ack.acknowledge();
    }

    @KafkaListener(topics = {
        EventType.Topic.RECEIPT_OCR_RESPONSE,
        EventType.Topic.NOTIFICATION_SEND
    })
    public void consume3(String message, Acknowledgment ack) {
        handleEvent(message);
        ack.acknowledge();
    }

    private EventHandler findEventHandler(Event<EventPayload> event) {
        for (EventHandler handler : eventHandlers) {
            if (handler.supports(event)) {
                return handler;
            }
        }
        return null;
    }

    private void handleEvent(String message) {
        Event<EventPayload> event = Event.fromJson(message);
        log.info("[KafkaConsumer.consume] Consumed event \n {} \n {}",
            kv("eventType", event.getEventType()),
            kv("payload", event.getPayload())
        );
        findEventHandler(event).handle(event);
    }
}