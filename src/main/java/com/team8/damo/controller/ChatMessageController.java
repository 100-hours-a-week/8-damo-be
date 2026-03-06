package com.team8.damo.controller;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Traced;
import com.team8.damo.controller.docs.ChatMessageControllerDocs;
import com.team8.damo.controller.request.ChatMessagePageRequest;
import com.team8.damo.controller.request.ChatMessageRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.ChatService;
import com.team8.damo.service.response.ChatMessagePageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class ChatMessageController implements ChatMessageControllerDocs {

    private final ChatService chatService;

    @MessageMapping("/message/{lightningId}")
    @Traced(value = "STOMP SEND /pub/message", type = "messaging")
    public void sendMessage(
        @DestinationVariable Long lightningId,
        Authentication authentication,
        ChatMessageRequest request
    ) {
        ElasticApm.currentSpan().setLabel("lightningId", lightningId);
        JwtUserDetails user = (JwtUserDetails) authentication.getPrincipal();
        chatService.createChatMessage(user.getUserId(), user.getNickname(), lightningId, request, LocalDateTime.now());
    }

    @Override
    @GetMapping("/api/v1/lightnings/{lightningId}/chat-messages")
    public BaseResponse<ChatMessagePageResponse> getChatMessages(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long lightningId,
        @Valid @ModelAttribute ChatMessagePageRequest request
    ) {
        return BaseResponse.ok(
            chatService.getChatMessages(user.getUserId(), lightningId, request.toServiceRequest())
        );
    }
}
