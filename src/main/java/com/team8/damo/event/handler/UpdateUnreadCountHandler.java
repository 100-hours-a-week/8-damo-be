package com.team8.damo.event.handler;

import com.team8.damo.chat.message.WsEventMessage;
import com.team8.damo.chat.producer.ChatProducer;
import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.UpdateUnreadCountEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UpdateUnreadCountHandler implements EventHandler<UpdateUnreadCountEventPayload> {

    private final ChatProducer chatProducer;

    @Override
    public void handle(Event<UpdateUnreadCountEventPayload> event) {
        UpdateUnreadCountEventPayload payload = event.getPayload();
        chatProducer.send(WsEventMessage.createUnreadUpdate(
            payload.lightningId(),
            payload
        ));
    }

    @Override
    public boolean supports(Event<UpdateUnreadCountEventPayload> event) {
        return EventType.UPDATE_UNREAD_COUNT == event.getEventType();
    }
}
