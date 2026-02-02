package com.team8.damo.event.handler;

import com.team8.damo.client.AiService;
import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.UserPersonaPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPersonaHandler implements EventHandler<UserPersonaPayload> {
    private final AiService aiService;

    @Override
    public void handle(Event<UserPersonaPayload> event) {
        UserPersonaPayload payload = event.getPayload();
        aiService.userPersonaUpdate(
            payload.user(),
            payload.allergies(),
            payload.likeFoods(),
            payload.likeIngredients()
        );
    }

    @Override
    public boolean supports(Event<UserPersonaPayload> event) {
        return EventType.USER_PERSONA == event.getEventType();
    }
}
