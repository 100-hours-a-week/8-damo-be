package com.team8.damo.event;

public record KafkaEvent(
    String topic,
    String payload
) {
}
