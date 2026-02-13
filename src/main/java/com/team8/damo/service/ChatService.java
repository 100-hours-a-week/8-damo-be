package com.team8.damo.service;

import com.team8.damo.chat.producer.ChatProducer;
import com.team8.damo.controller.request.ChatMessageRequest;
import com.team8.damo.entity.ChatMessage;
import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.Direction;
import com.team8.damo.event.EventType;
import com.team8.damo.event.handler.CommonEventPublisher;
import com.team8.damo.event.payload.CreateChatMessageEventPayload;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.ChatMessageRepository;
import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.repository.LightningRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.ChatMessagePageServiceRequest;
import com.team8.damo.service.response.ChatMessagePageResponse;
import com.team8.damo.service.response.ChatMessagePageResponse.InitialScrollMode;
import com.team8.damo.service.response.ChatMessagePageResponse.MessageItem;
import com.team8.damo.service.response.ChatMessagePageResponse.PageInfo;
import com.team8.damo.service.response.ChatMessagePageResponse.PageParam;
import com.team8.damo.service.response.ChatMessagePageResponse.ReadBoundary;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final Snowflake snowflake;
    private final ChatProducer chatProducer;
    private final UserRepository userRepository;
    private final LightningRepository lightningRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LightningParticipantRepository lightningParticipantRepository;
    private final CommonEventPublisher commonEventPublisher;

    @Transactional
    public void createChatMessage(Long senderId, Long lightningId, ChatMessageRequest request, LocalDateTime currentTime) {
        User sender = findUserBy(senderId);
        Lightning lightning = findLightningBy(lightningId);

        ChatMessage chatMessage = ChatMessage.builder()
            .id(snowflake.nextId())
            .user(sender)
            .lightning(lightning)
            .content(request.content())
            .build();
        chatMessageRepository.save(chatMessage);

        commonEventPublisher.publish(
            EventType.CREATE_CHAT_MESSAGE,
            CreateChatMessageEventPayload.builder()
                .messageId(chatMessage.getId())
                .senderId(senderId)
                .lightningId(lightningId)
                .chatType(request.chatType())
                .content(request.content())
                .createdAt(currentTime)
                .senderNickname(sender.getNickname())
                .build()
        );
    }

    @Transactional
    public ChatMessagePageResponse getChatMessages(Long userId, Long lightningId, ChatMessagePageServiceRequest request) {
        LightningParticipant participant = lightningParticipantRepository.findByLightningIdAndUserId(lightningId, userId)
            .orElseThrow(() -> new CustomException(LIGHTNING_PARTICIPANT_NOT_FOUND));

        Long anchorCursor = participant.getLastReadChatMessageId() == null ? 0L : participant.getLastReadChatMessageId();
        Direction direction = request.direction();
        Long cursorId = request.cursorId();
        int size = request.size();

        if (cursorId == null) {
            ChatMessagePageResponse response = buildInitialResponse(lightningId, anchorCursor, size);
            updateLastReadToLatest(participant, lightningId);
            return response;
        }
        Direction effectiveDirection = direction == null ? Direction.NEXT : direction;
        return buildDirectionalResponse(lightningId, effectiveDirection, cursorId, size, anchorCursor);
    }

    private ChatMessagePageResponse buildInitialResponse(Long lightningId, Long anchorCursor, int size) {
        Long latestMessageId = chatMessageRepository.findLatestMessageId(lightningId);

        if (latestMessageId == null) {
            return buildResponse(
                List.of(), null, null, false, false, size, anchorCursor,
                InitialScrollMode.TOP,
                new ReadBoundary(false, null, null)
            );
        }

        if (anchorCursor <= 0L) {
            return buildTopInitialResponse(lightningId, anchorCursor, size);
        }

        if (latestMessageId <= anchorCursor) {
            return buildBottomInitialResponse(lightningId, anchorCursor, size);
        }

        return buildCenteredInitialResponse(lightningId, anchorCursor, size);
    }

    private ChatMessagePageResponse buildTopInitialResponse(Long lightningId, Long anchorCursor, int size) {
        MessageSlice nextSlice = fetchNextSlice(lightningId, 0L, size);
        return buildResponse(
            nextSlice.messages(), null, null, false, nextSlice.hasMore(), size, anchorCursor,
            InitialScrollMode.TOP,
            new ReadBoundary(false, null, null)
        );
    }

    private ChatMessagePageResponse buildBottomInitialResponse(Long lightningId, Long anchorCursor, int size) {
        MessageSlice prevSlice = fetchPrevSlice(lightningId, Long.MAX_VALUE, size);
        Long displayedLastRead = prevSlice.messages().isEmpty() ? null : prevSlice.messages().getLast().getId();

        return buildResponse(
            prevSlice.messages(), null, null, prevSlice.hasMore(), false, size, anchorCursor,
            InitialScrollMode.BOTTOM,
            new ReadBoundary(false, displayedLastRead, null)
        );
    }

    private ChatMessagePageResponse buildCenteredInitialResponse(Long lightningId, Long anchorCursor, int size) {
        MessageSlice prevSlice = fetchPrevOrEqualSlice(lightningId, anchorCursor, size);
        MessageSlice nextSlice = fetchNextSlice(lightningId, anchorCursor, size);

        List<ChatMessage> prevMessages = prevSlice.messages();
        List<ChatMessage> nextMessages = nextSlice.messages();

        int targetRight = size / 2;
        int targetLeft = size - targetRight;

        if (prevMessages.size() < targetLeft) {
            targetLeft = prevMessages.size();
            targetRight = Math.min(nextMessages.size(), size - targetLeft);
        } else if (nextMessages.size() < targetRight) {
            targetRight = nextMessages.size();
            targetLeft = Math.min(prevMessages.size(), size - targetRight);
        }

        List<ChatMessage> selectedLeft = takeLast(prevMessages, targetLeft);
        List<ChatMessage> selectedRight = takeFirst(nextMessages, targetRight);

        int remaining = size - selectedLeft.size() - selectedRight.size();
        if (remaining > 0) {
            List<ChatMessage> nextRemainder = nextMessages.subList(selectedRight.size(), nextMessages.size());
            List<ChatMessage> nextExtra = takeFirst(nextRemainder, remaining);

            List<ChatMessage> rightCombined = new ArrayList<>(selectedRight);
            rightCombined.addAll(nextExtra);
            selectedRight = rightCombined;
            remaining -= nextExtra.size();
        }

        if (remaining > 0) {
            int remainingPrevEnd = Math.max(0, prevMessages.size() - selectedLeft.size());
            List<ChatMessage> prevRemainder = prevMessages.subList(0, remainingPrevEnd);
            List<ChatMessage> prevExtra = takeLast(prevRemainder, remaining);

            List<ChatMessage> leftCombined = new ArrayList<>(prevExtra);
            leftCombined.addAll(selectedLeft);
            selectedLeft = leftCombined;
        }

        List<ChatMessage> merged = new ArrayList<>(selectedLeft);
        merged.addAll(selectedRight);
        merged.sort(Comparator.comparing(ChatMessage::getId));

        boolean hasPreviousPage = prevSlice.hasMore() || prevMessages.size() > selectedLeft.size();
        boolean hasNextPage = nextSlice.hasMore() || nextMessages.size() > selectedRight.size();

        Long lastReadMessageId = selectedLeft.isEmpty() ? null : selectedLeft.getLast().getId();
        Long firstUnreadMessageId = selectedRight.isEmpty() ? null : selectedRight.getFirst().getId();
        boolean showDivider = lastReadMessageId != null && firstUnreadMessageId != null;

        return buildResponse(
            merged, null, null, hasPreviousPage, hasNextPage, size, anchorCursor,
            InitialScrollMode.CENTER,
            new ReadBoundary(showDivider, lastReadMessageId, firstUnreadMessageId)
        );
    }

    private ChatMessagePageResponse buildDirectionalResponse(Long lightningId, Direction direction, Long cursorId, int size, Long anchorCursor) {
        if (direction == Direction.PREV) {
            MessageSlice prevSlice = fetchPrevSlice(lightningId, cursorId, size);
            return buildResponse(
                prevSlice.messages(), Direction.PREV, cursorId, prevSlice.hasMore(), false, size, anchorCursor,
                InitialScrollMode.NONE,
                new ReadBoundary(false, null, null)
            );
        } else {
            MessageSlice nextSlice = fetchNextSlice(lightningId, cursorId, size);
            return buildResponse(
                nextSlice.messages(), Direction.NEXT, cursorId, false, nextSlice.hasMore(), size, anchorCursor,
                InitialScrollMode.NONE,
                new ReadBoundary(false, null, null)
            );
        }
    }

    private MessageSlice fetchNextSlice(Long lightningId, Long cursorId, int size) {
        List<ChatMessage> raw = chatMessageRepository.findNextMessages(
            lightningId, cursorId,
            PageRequest.of(0, size + 1, Sort.by(Sort.Direction.ASC, "id"))
        );
        boolean hasMore = raw.size() > size;
        List<ChatMessage> messages = hasMore ? raw.subList(0, size) : raw;
        return new MessageSlice(messages, hasMore);
    }

    private MessageSlice fetchPrevSlice(Long lightningId, Long cursorId, int size) {
        List<ChatMessage> raw = chatMessageRepository.findPrevMessages(
            lightningId, cursorId,
            PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "id"))
        );
        return toPrevSlice(raw, size);
    }

    private MessageSlice fetchPrevOrEqualSlice(Long lightningId, Long cursorId, int size) {
        List<ChatMessage> raw = chatMessageRepository.findPrevOrEqualMessages(
            lightningId, cursorId,
            PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "id"))
        );
        return toPrevSlice(raw, size);
    }

    private MessageSlice toPrevSlice(List<ChatMessage> raw, int size) {
        boolean hasMore = raw.size() > size;
        List<ChatMessage> nearest = hasMore ? raw.subList(0, size) : raw;

        List<ChatMessage> ascMessages = new ArrayList<>(nearest);
        ascMessages.sort(Comparator.comparing(ChatMessage::getId));
        return new MessageSlice(ascMessages, hasMore);
    }

    private List<ChatMessage> takeFirst(List<ChatMessage> messages, int count) {
        if (count <= 0 || messages.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(messages.subList(0, Math.min(count, messages.size())));
    }

    private List<ChatMessage> takeLast(List<ChatMessage> messages, int count) {
        if (count <= 0 || messages.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    private void updateLastReadToLatest(LightningParticipant participant, Long lightningId) {
        Long latestMessageId = chatMessageRepository.findLatestMessageId(lightningId);
        if (latestMessageId == null) {
            return;
        }
        Long currentLastReadId = participant.getLastReadChatMessageId() == null ? 0L : participant.getLastReadChatMessageId();
        participant.updateLastReadChatMessageId(Math.max(currentLastReadId, latestMessageId));
    }

    private ChatMessagePageResponse buildResponse(
        List<ChatMessage> messages,
        Direction requestDirection,
        Long requestCursorId,
        boolean hasPreviousPage,
        boolean hasNextPage,
        int size,
        Long anchorCursor,
        InitialScrollMode initialScrollMode,
        ReadBoundary readBoundary
    ) {
        List<MessageItem> items = messages.stream()
            .map(cm -> new MessageItem(
                cm.getId(),
                cm.getUser().getId(),
                cm.getUser().getNickname(),
                cm.getContent(),
                cm.getCreatedAt()
            ))
            .toList();

        Long prevCursor = messages.isEmpty() ? null : messages.getFirst().getId();
        Long nextCursor = messages.isEmpty() ? null : messages.getLast().getId();

        PageParam previousPageParam = hasPreviousPage ? new PageParam(Direction.PREV, prevCursor) : null;
        PageParam nextPageParam = hasNextPage ? new PageParam(Direction.NEXT, nextCursor) : null;

        PageInfo pageInfo = new PageInfo(
            requestDirection, requestCursorId,
            prevCursor, nextCursor,
            hasPreviousPage, hasNextPage,
            previousPageParam, nextPageParam,
            size
        );

        InitialScrollMode mode = initialScrollMode == null ? InitialScrollMode.NONE : initialScrollMode;
        ReadBoundary boundary = readBoundary == null ? new ReadBoundary(false, null, null) : readBoundary;

        return new ChatMessagePageResponse(items, pageInfo, anchorCursor, mode, boundary);
    }

    private record MessageSlice(
        List<ChatMessage> messages,
        boolean hasMore
    ) {
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
