package com.team8.damo.chat.consumer;

import com.team8.damo.chat.message.ChatBroadcastMessage;
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
            ChatBroadcastMessage message = objectMapper.readValue(jsonMessage, ChatBroadcastMessage.class);
            messageSendingOperations.convertAndSend("/sub/lightning/" + message.lightningId(), message);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
