package com.team8.damo.security.handler;

import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.security.jwt.JwtProvider;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.LightningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

import static com.team8.damo.redis.key.RedisKeyPrefix.LIGHTNING_SUBSCRIBE_USERS;
import static com.team8.damo.redis.key.RedisKeyPrefix.STOMP_SUBSCRIPTION;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SEND_DEST =
        Pattern.compile("^/pub/message/(\\d+)$");

    private static final Pattern SUB_DEST =
        Pattern.compile("^/sub/lightning/(\\d+)$");

    private final RedisTemplate<String, String> redisTemplate;

    private final JwtProvider jwtProvider;
    private final LightningService lightningService;
    private final LightningParticipantRepository lightningParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand command = accessor.getCommand();
        switch (command) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> addSubscriber(accessor, command);
            case UNSUBSCRIBE -> removeSubscriber(accessor, command);
            case SEND -> authorize(accessor, command);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) {
            throw new AccessDeniedException("Invalid JWT token");
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

        String sessionKey = accessor.getSessionId() + ":" + accessor.getSubscriptionId();
        redisTemplate.opsForValue().set(
            STOMP_SUBSCRIPTION.key(sessionKey), lightningId.toString()
        );

        redisTemplate.opsForSet().add(
            LIGHTNING_SUBSCRIBE_USERS.key(lightningId),
            user.getUserId().toString()
        );

        lightningService.onSubscribe(user.getUserId(), lightningId);
    }

    private void removeSubscriber(StompHeaderAccessor accessor, StompCommand command) {
        String key = STOMP_SUBSCRIPTION.key(accessor.getSessionId(), accessor.getSubscriptionId());
        String lightningIdStr = redisTemplate.opsForValue().get(key);

        if (lightningIdStr == null) {
            log.warn("No subscription mapping found for {}", key);
            return;
        }

        JwtUserDetails user = extractUser(accessor);
        Long lightningId = Long.valueOf(lightningIdStr);

        redisTemplate.opsForSet().remove(
            LIGHTNING_SUBSCRIBE_USERS.key(lightningId),
            user.getUserId().toString()
        );

        lightningService.onUnsubscribe(user.getUserId(), lightningId);
        redisTemplate.delete(key);
    }

    private JwtUserDetails extractUser(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
            || !(authentication.getPrincipal() instanceof JwtUserDetails user)
        ) {
            throw new AccessDeniedException("Unauthenticated STOMP session");
        }
        return user;
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
