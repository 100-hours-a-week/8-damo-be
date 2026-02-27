package com.team8.damo.service.response;

import java.util.List;

public record CursorPageResponse<T>(
    List<T> data,
    Long nextCursor,
    boolean hasNext
) {
}
