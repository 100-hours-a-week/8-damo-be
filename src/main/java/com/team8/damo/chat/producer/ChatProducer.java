package com.team8.damo.chat.producer;

import com.team8.damo.chat.message.ChatBroadcastMessage;

public interface ChatProducer {
    void send(ChatBroadcastMessage message);
}
