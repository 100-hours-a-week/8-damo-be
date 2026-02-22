package com.team8.damo.chat.producer;

import com.team8.damo.chat.message.WsEventMessage;

public interface ChatMessageBroker {
    void send(WsEventMessage message);
}
