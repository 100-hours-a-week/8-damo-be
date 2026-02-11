package com.team8.damo.service;

import com.team8.damo.chat.producer.ChatProducer;
import com.team8.damo.controller.request.ChatMessageRequest;
import com.team8.damo.entity.ChatMessage;
import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.User;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.ChatMessageRepository;
import com.team8.damo.repository.LightningRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team8.damo.exception.errorcode.ErrorCode.LIGHTNING_NOT_FOUND;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final Snowflake snowflake;
    private final ChatProducer chatProducer;
    private final UserRepository userRepository;
    private final LightningRepository lightningRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public void createChatMessage(Long senderId, Long lightningId, ChatMessageRequest request) {
        chatProducer.send(senderId, lightningId, request);

        log.info("[ChatService.createChatMessage] Lightning Id : {} SenderId : {}",  lightningId, senderId);
        User sender = findUserBy(senderId);
        Lightning lightning = findLightningBy(lightningId);

        ChatMessage chatMessage = ChatMessage.builder()
            .id(snowflake.nextId())
            .user(sender)
            .lightning(lightning)
            .content(request.content())
            .build();
        chatMessageRepository.save(chatMessage);
    }

    private User findUserBy(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private Lightning findLightningBy(Long lightningId) {
        return lightningRepository.findById(lightningId)
            .orElseThrow(() -> new CustomException(LIGHTNING_NOT_FOUND));
    }
}
