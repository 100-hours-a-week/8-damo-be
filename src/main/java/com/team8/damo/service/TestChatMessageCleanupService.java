package com.team8.damo.service;

import com.team8.damo.controller.request.TestChatMessageCleanupRequest;
import com.team8.damo.controller.response.TestChatMessageCleanupResponse;
import com.team8.damo.repository.ChatMessageRepository;
import com.team8.damo.repository.ReadStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
@RequiredArgsConstructor
public class TestChatMessageCleanupService {

    private static final String ALLOWED_PREFIX = "k6-chat";

    private final ChatMessageRepository chatMessageRepository;
    private final ReadStatusRepository readStatusRepository;

    @Value("${test-data.cleanup.token:}")
    private String cleanupToken;

    @Transactional
    public TestChatMessageCleanupResponse cleanup(String requestToken, TestChatMessageCleanupRequest request) {
        validateToken(requestToken);

        String prefix = normalizePrefix(request.prefix());
        long chatMessageCount = chatMessageRepository.countByContentPrefix(prefix);
        long readStatusCount = readStatusRepository.countByChatMessageContentPrefix(prefix);

        if (request.dryRun()) {
            return new TestChatMessageCleanupResponse(prefix, true, chatMessageCount, readStatusCount, 0, 0);
        }

        long deletedReadStatusCount = readStatusRepository.deleteAllByChatMessageContentPrefix(prefix);
        long deletedChatMessageCount = chatMessageRepository.deleteAllByContentPrefix(prefix);

        return new TestChatMessageCleanupResponse(
            prefix,
            false,
            chatMessageCount,
            readStatusCount,
            deletedChatMessageCount,
            deletedReadStatusCount
        );
    }

    private void validateToken(String requestToken) {
        if (cleanupToken == null || cleanupToken.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Cleanup token is not configured.");
        }
        if (requestToken == null || requestToken.isBlank() || !cleanupToken.equals(requestToken)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid cleanup token.");
        }
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Cleanup prefix is required.");
        }

        String normalized = prefix.trim();
        if (!normalized.startsWith(ALLOWED_PREFIX)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cleanup prefix must start with 'k6-chat'.");
        }
        return normalized;
    }
}
