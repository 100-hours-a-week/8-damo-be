package com.team8.damo.chat.producer;

import com.team8.damo.controller.request.ChatMessageRequest;
import com.team8.damo.chat.message.ChatBroadcastMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static com.team8.damo.config.RedisConfig.CHANNEL;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPublisher implements ChatProducer {
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public void send(ChatBroadcastMessage message) {
        try {
            stringRedisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
