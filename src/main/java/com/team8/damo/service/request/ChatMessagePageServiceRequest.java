package com.team8.damo.service.request;

import com.team8.damo.entity.enumeration.Direction;

public record ChatMessagePageServiceRequest(Direction direction, Long cursorId, int size) {
}
