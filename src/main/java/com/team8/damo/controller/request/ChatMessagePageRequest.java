package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.Direction;
import com.team8.damo.service.request.ChatMessagePageServiceRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessagePageRequest {

    private Direction direction;

    private Long cursorId;

    @Min(value = 1, message = "size는 최소 1 이상이어야 합니다.")
    @Max(value = 100, message = "size는 최대 100 이하여야 합니다.")
    private Integer size = 20;

    public ChatMessagePageServiceRequest toServiceRequest() {
        return new ChatMessagePageServiceRequest(direction, cursorId, size);
    }
}
