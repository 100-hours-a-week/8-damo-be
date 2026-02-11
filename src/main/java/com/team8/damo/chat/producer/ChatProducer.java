package com.team8.damo.chat.producer;

import com.team8.damo.controller.request.ChatMessageRequest;

public interface ChatProducer {
    void send(Long senderId, Long lightningId, ChatMessageRequest request);
}
