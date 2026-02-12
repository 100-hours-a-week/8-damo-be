package com.team8.damo.event.handler;

import com.team8.damo.chat.message.ChatBroadcastMessage;
import com.team8.damo.chat.producer.ChatProducer;
import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.CreateChatMessageEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateChatMessageHandler implements EventHandler<CreateChatMessageEventPayload> {
    private final ChatProducer chatProducer;

    @Override
    public void handle(Event<CreateChatMessageEventPayload> event) {
        CreateChatMessageEventPayload payload = event.getPayload();
        chatProducer.send(ChatBroadcastMessage.from(payload));

        log.info("[ChatService.createChatMessage] {} {} {}",
            kv("messageId", payload.messageId()),
            kv("lightningId", payload.lightningId()),
            kv("senderId", payload.senderId())
        );
    }

    @Override
    public boolean supports(Event<CreateChatMessageEventPayload> event) {
        return EventType.CREATE_CHAT_MESSAGE == event.getEventType();
    }
}
