package com.team8.damo.event.handler;

import com.team8.damo.client.AiService;
import com.team8.damo.event.Event;
import com.team8.damo.event.KafkaEvent;
import com.team8.damo.event.payload.EventPayload;
import com.team8.damo.service.RecommendRestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventHandler {

    private final AiService aiService;
    private final RecommendRestaurantService recommendRestaurantService;
    private final List<EventHandler> eventHandlers;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Async("eventRelayExecutor")
    @Retryable(
        delay = 200L,
        multiplier = 1.5
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(Event<EventPayload> event) {
        handleEvent(event);
    }

    @Async("eventRelayExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void produceKafkaEvent(KafkaEvent kafkaEvent) {
        try {
            kafkaTemplate.send(
                kafkaEvent.topic(),
                kafkaEvent.payload()
            ).get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[MessageRelay.produceKafkaEvent] kafkaEvent={}", kafkaEvent, e);
        }
    }

    public void handleEvent(Event<EventPayload> event) {
        for (EventHandler eventHandler : eventHandlers) {
            if (eventHandler.supports(event)) {
                eventHandler.handle(event);
            }
        }
    }
}
