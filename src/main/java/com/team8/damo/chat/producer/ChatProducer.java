package com.team8.damo.chat.producer;

import com.team8.damo.chat.message.WsEventMessage;

public interface ChatProducer {
    void send(WsEventMessage message);
}
