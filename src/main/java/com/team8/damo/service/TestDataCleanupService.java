package com.team8.damo.service;

import com.team8.damo.controller.request.TestDataCleanupRequest;
import com.team8.damo.controller.response.TestDataCleanupResponse;
import com.team8.damo.repository.ChatMessageRepository;
import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.repository.LightningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
@RequiredArgsConstructor
public class TestDataCleanupService {

    private static final List<String> ALLOWED_PREFIXES = List.of("k6-", "loadtest-", "lt-perm-", "lt-v2-");

    private final LightningRepository lightningRepository;
    private final LightningParticipantRepository lightningParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Value("${test-data.cleanup.token}")
    private String cleanupToken;

    @Transactional
    public TestDataCleanupResponse cleanup(String requestToken, TestDataCleanupRequest request) {
        validateToken(requestToken);

        String prefix = normalizePrefix(request.prefix());
        List<Long> lightningIds = lightningRepository.findIdsByDescriptionPrefix(prefix);

        long lightningCount = lightningIds.size();
        if (lightningIds.isEmpty()) {
            return new TestDataCleanupResponse(prefix, request.dryRun(), 0, 0, 0, 0, 0, 0);
        }

        long participantCount = lightningParticipantRepository.countByLightningIdIn(lightningIds);
        long chatMessageCount = chatMessageRepository.countByLightningIdIn(lightningIds);

        if (request.dryRun()) {
            return new TestDataCleanupResponse(
                prefix,
                true,
                lightningCount,
                participantCount,
                chatMessageCount,
                0,
                0,
                0
            );
        }

        long deletedChatMessageCount = chatMessageRepository.deleteAllByLightningIds(lightningIds);
        long deletedParticipantCount = lightningParticipantRepository.deleteAllByLightningIds(lightningIds);
        lightningRepository.deleteAllByIdInBatch(lightningIds);

        return new TestDataCleanupResponse(
            prefix,
            false,
            lightningCount,
            participantCount,
            chatMessageCount,
            lightningCount,
            deletedParticipantCount,
            deletedChatMessageCount
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
        boolean allowed = ALLOWED_PREFIXES.stream().anyMatch(normalized::startsWith);
        if (!allowed) {
            throw new ResponseStatusException(BAD_REQUEST, "Cleanup prefix must start with an allowed test prefix.");
        }
        return normalized;
    }
}
