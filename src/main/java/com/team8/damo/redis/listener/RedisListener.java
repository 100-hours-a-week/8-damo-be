package com.team8.damo.redis.listener;

import com.team8.damo.chat.message.WsEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messageSendingOperations;

    public void onMessage(String jsonMessage) {
        try {
            WsEventMessage message = objectMapper.readValue(jsonMessage, WsEventMessage.class);
            messageSendingOperations.convertAndSend("/sub/lightning/" + message.lightningId(), message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
