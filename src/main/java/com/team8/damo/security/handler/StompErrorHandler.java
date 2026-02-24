package com.team8.damo.security.handler;

import com.beust.jcommander.internal.Nullable;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import tools.jackson.databind.ObjectMapper;

@Component("stompSubProtocolErrorHandler")
@RequiredArgsConstructor
public class StompErrorHandler extends StompSubProtocolErrorHandler {
    private final ObjectMapper objectMapper;

    @Override
    public Message<byte[]> handleClientMessageProcessingError(
        @Nullable Message<byte[]> clientMessage,
        Throwable ex
    ) {
        CustomException ce = unwrapCustomException(ex);
        if (ce == null) {
            return
                super.handleClientMessageProcessingError(clientMessage,
                    ex);
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(ce.getMessage());
        accessor.setNativeHeader("x-http-status", String.valueOf(ce.getHttpStatus().value()));
        accessor.setNativeHeader("x-error-code", ce.getErrorCode().name());
        accessor.setLeaveMutable(true);

        byte[] body = toBytes(BaseResponse.fail(ce));
        return MessageBuilder.createMessage(body, accessor.getMessageHeaders());
    }

    private CustomException unwrapCustomException(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof CustomException ce) return
                ce;
            t = t.getCause();
        }
        return null;
    }

    private byte[] toBytes(Object value) {
        try { return
            objectMapper.writeValueAsBytes(value); }
        catch (Exception e) { return new byte[0]; }
    }
}