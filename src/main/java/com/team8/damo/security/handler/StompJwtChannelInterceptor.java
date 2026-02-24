package com.team8.damo.security.handler;

import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.security.jwt.JwtProvider;
import com.team8.damo.security.jwt.JwtUserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SEND_DEST =
        Pattern.compile("^/pub/message/(\\d+)$");

    private static final Pattern SUB_DEST =
        Pattern.compile("^/sub/lightning/(\\d+)$");

    private final JwtProvider jwtProvider;
    private final StompSubscriptionCleanupManager subscriptionCleanupManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand command = accessor.getCommand();
        switch (command) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> addSubscriber(accessor, command);
            case UNSUBSCRIBE -> removeSubscriber(accessor);
            case DISCONNECT -> removeAllSubscribers(accessor);
            case SEND -> authorize(accessor, command);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.JWT_FILTER_ERROR);
        }

        String token = authHeader.substring(7);
        try {
            jwtProvider.validateToken(token);
            accessor.setUser(jwtProvider.getAuthentication(token));
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            throw new CustomException(ErrorCode.JWT_INVALID_TOKEN_ERROR);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.JWT_EXPIRED_TOKEN_ERROR);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(ErrorCode.JWT_UNSUPPORTED_TOKEN_ERROR);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.JWT_CLAIMS_EMPTY_ERROR);
        }

        Authentication authentication = jwtProvider.getAuthentication(token);
        accessor.setUser(authentication); // 이후 SEND/SUBSCRIBE / Controller에서 Principal 사용 가능
    }

    private JwtUserDetails authorize(StompHeaderAccessor accessor, StompCommand command) {
        JwtUserDetails user = extractUser(accessor);

        String destination = accessor.getDestination();
        if (destination == null)
            throw new AccessDeniedException("Missing destination");

        log.info("[ChannelInterceptor.authorize] userId : {}", user.getUserId());

        return user;
    }

    private void addSubscriber(StompHeaderAccessor accessor, StompCommand command) {
        JwtUserDetails user = authorize(accessor, command);
        Long lightningId = extractLightningId(accessor.getDestination(), command);

        subscriptionCleanupManager.registerSubscription(
            user.getUserId(),
            accessor.getSessionId(),
            accessor.getSubscriptionId(),
            lightningId
        );
    }

    private void removeSubscriber(StompHeaderAccessor accessor) {
        JwtUserDetails user = extractUser(accessor);
        subscriptionCleanupManager.unregisterSubscription(
            user.getUserId(),
            accessor.getSessionId(),
            accessor.getSubscriptionId()
        );
    }

    private void removeAllSubscribers(StompHeaderAccessor accessor) {
        subscriptionCleanupManager.unregisterAllBySession(
            extractUserIdOrNull(accessor),
            accessor.getSessionId()
        );
    }

    private JwtUserDetails extractUser(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
            || !(authentication.getPrincipal() instanceof JwtUserDetails user)
        ) {
            throw new AccessDeniedException("Unauthenticated STOMP session");
        }
        return user;
    }

    private Long extractUserIdOrNull(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication
            && authentication.getPrincipal() instanceof JwtUserDetails user) {
            return user.getUserId();
        }
        return null;
    }

    private Long extractLightningId(String destination, StompCommand command) {
        Pattern pattern = (command == StompCommand.SEND) ? SEND_DEST : SUB_DEST;
        Matcher matcher = pattern.matcher(destination);
        if (!matcher.matches()) {
            throw new AccessDeniedException("Unsupported destination:" + destination);
        }
        return Long.valueOf(matcher.group(1));
    }
}
