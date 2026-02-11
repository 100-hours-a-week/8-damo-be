package com.team8.damo.security.handler;

import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.security.jwt.JwtProvider;
import com.team8.damo.security.jwt.JwtUserDetails;
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
    private final LightningParticipantRepository lightningParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (command == StompCommand.SEND ||
            command == StompCommand.SUBSCRIBE) {
            authorize(accessor, command);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        log.info("[ChatService.createChatMessage] Authorization Header : {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing Authorization header");
        }

        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) {
            throw new AccessDeniedException("Invalid JWT token");
        }

        Authentication authentication = jwtProvider.getAuthentication(token);
        accessor.setUser(authentication); // 이후 SEND/SUBSCRIBE / Controller에서 Principal 사용 가능
        log.info("[ChannelInterceptor.authenticate] Authentication : {}", authentication);
    }

    private void authorize(StompHeaderAccessor accessor, StompCommand command) {
        if (!(accessor.getUser() instanceof Authentication authentication)
            || !(authentication.getPrincipal() instanceof JwtUserDetails user)
        ) {
            throw new AccessDeniedException("Unauthenticated STOMP session");
        }

        String destination = accessor.getDestination();
        if (destination == null)
            throw new AccessDeniedException("Missing destination");

        log.info("[ChannelInterceptor.authorize] userId : {}", user.getUserId());

        Long lightningId = extractLightningId(destination, command);
        boolean participant = lightningParticipantRepository
                .existsByLightningIdAndUserId(lightningId, user.getUserId());

        if (!participant) {
            throw new AccessDeniedException("Not a lightning participant");
        }
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
