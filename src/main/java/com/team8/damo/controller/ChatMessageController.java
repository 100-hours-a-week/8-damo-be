package com.team8.damo.controller;

import com.team8.damo.controller.request.ChatMessageRequest;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;

    @MessageMapping("/message/{lightningId}")
    public void sendMessage(
        @DestinationVariable Long lightningId,
        Authentication authentication,
        ChatMessageRequest request
    ) {
        JwtUserDetails user = (JwtUserDetails) authentication.getPrincipal();
        chatService.createChatMessage(user.getUserId(), lightningId, request, LocalDateTime.now());
    }
}
