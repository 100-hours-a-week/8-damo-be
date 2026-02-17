package com.team8.damo.service.response;

import com.team8.damo.entity.enumeration.Direction;

import java.time.LocalDateTime;
import java.util.List;

public record ChatMessagePageResponse(
    List<MessageItem> messages,
    PageInfo pageInfo,
    Long anchorCursor,
    InitialScrollMode initialScrollMode,
    ReadBoundary readBoundary
) {

    public record MessageItem(
        Long messageId,
        Long senderId,
        String senderNickname,
        String content,
        LocalDateTime createdAt,
        Long unreadCount
    ) {
    }

    public record PageInfo(
        Direction requestDirection,
        Long requestCursorId,
        Long prevCursor,
        Long nextCursor,
        boolean hasPreviousPage,
        boolean hasNextPage,
        PageParam previousPageParam,
        PageParam nextPageParam,
        int size
    ) {
    }

    public record PageParam(
        Direction direction,
        Long cursorId
    ) {
    }

    public enum InitialScrollMode {
        TOP,
        CENTER,
        BOTTOM,
        NONE
    }

    public record ReadBoundary(
        boolean showDivider,
        Long lastReadMessageId,
        Long firstUnreadMessageId
    ) {
    }
}
