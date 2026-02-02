package com.team8.damo.event;

import com.team8.damo.event.payload.EventPayload;
import com.team8.damo.util.DataSerializer;
import lombok.Getter;

@Getter
public class Event<T extends EventPayload> {
    private EventType eventType;
    private T payload;

    public static Event<EventPayload> of(EventType eventType, EventPayload payload) {
        Event<EventPayload> event = new Event<>();
        event.eventType = eventType;
        event.payload = payload;
        return event;
    }

    public String toJson() {
        return DataSerializer.serialize(this);
    }
}
