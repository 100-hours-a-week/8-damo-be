package com.team8.damo.event.handler;

import com.team8.damo.cache.store.LightningCacheService;
import com.team8.damo.chat.message.ChatBroadcastMessage;
import com.team8.damo.chat.message.WsEventMessage;
import com.team8.damo.chat.producer.ChatMessageBroker;
import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.CreateChatMessageEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.team8.damo.redis.key.RedisKeyPrefix.LIGHTNING_SUBSCRIBE_USERS;
import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateChatMessageHandler implements EventHandler<CreateChatMessageEventPayload> {
    private final ChatMessageBroker chatMessageBroker;
    private final LightningCacheService lightningCacheService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void handle(Event<CreateChatMessageEventPayload> event) {
        CreateChatMessageEventPayload payload = event.getPayload();

        long totalParticipant = lightningCacheService.getLightningParticipantCount(payload.lightningId());
        long userCount = redisTemplate.opsForSet().size(LIGHTNING_SUBSCRIBE_USERS.key(payload.lightningId()));

        chatMessageBroker.send(WsEventMessage.createChatMessage(
            payload.lightningId(),
            ChatBroadcastMessage.from(payload, totalParticipant - userCount))
        );
    }

    @Override
    public boolean supports(Event<CreateChatMessageEventPayload> event) {
        return EventType.CREATE_CHAT_MESSAGE == event.getEventType();
    }
}
