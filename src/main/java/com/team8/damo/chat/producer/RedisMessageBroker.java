package com.team8.damo.chat.producer;

import co.elastic.apm.api.CaptureTransaction;
import co.elastic.apm.api.ElasticApm;
import com.team8.damo.chat.message.WsEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static com.team8.damo.config.RedisConfig.CHANNEL;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageBroker implements ChatMessageBroker {
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SimpMessageSendingOperations messageSendingOperations;

    public void send(WsEventMessage message) {
        try {
            stringRedisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @CaptureTransaction(value = "redis.onMessage", type = "messaging")
    public void onMessage(String jsonMessage) {
        try {
            WsEventMessage message = objectMapper.readValue(jsonMessage, WsEventMessage.class);
            ElasticApm.currentTransaction().setLabel("lightningId", message.lightningId());
            messageSendingOperations.convertAndSend("/sub/lightning/" + message.lightningId(), message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
