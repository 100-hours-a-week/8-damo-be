package com.team8.damo.security.handler;

import com.team8.damo.security.jwt.JwtUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompSessionDisconnectEventListener {

    private final StompSubscriptionCleanupManager subscriptionCleanupManager;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        Long userId = extractUserId(event);

        subscriptionCleanupManager.unregisterAllBySession(userId, sessionId);
        log.info("STOMP session disconnected. sessionId={}, userId={}", sessionId, userId);
    }

    private Long extractUserId(SessionDisconnectEvent event) {
        if (event.getUser() instanceof Authentication authentication
            && authentication.getPrincipal() instanceof JwtUserDetails user) {
            return user.getUserId();
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof Authentication authentication
            && authentication.getPrincipal() instanceof JwtUserDetails user) {
            return user.getUserId();
        }
        return null;
    }
}
