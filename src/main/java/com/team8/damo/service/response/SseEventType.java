package com.team8.damo.service.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventType {
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    STREAMING("streaming"),
    DONE("done"),
    ERROR("error"),
    HEARTBEAT("heartbeat"),
    ;

    private final String value;
}
