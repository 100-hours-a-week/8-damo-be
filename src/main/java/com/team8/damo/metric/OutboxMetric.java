package com.team8.damo.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetric {

    private final Counter publishFailureCounter;

    public OutboxMetric(MeterRegistry meterRegistry) {
        this.publishFailureCounter =
            Counter.builder("outbox.publish.failures")
                .description("Number of failed outbox publish attempts")
                .register(meterRegistry);
    }

    public void incrementPublishFailure() {
        publishFailureCounter.increment();
    }
}