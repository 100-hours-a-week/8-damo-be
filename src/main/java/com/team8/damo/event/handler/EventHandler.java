package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.payload.EventPayload;

public interface EventHandler<T extends EventPayload> {
    void handle(Event<T> event);
    boolean supports(Event<T> event);
}
