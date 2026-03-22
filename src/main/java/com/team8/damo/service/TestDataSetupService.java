package com.team8.damo.service;

import com.team8.damo.controller.request.TestDataSetupRequest;
import com.team8.damo.controller.response.TestDataSetupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataSetupService {

    private final TestDataSetupExecutor executor;

    @Value("${test-data.cleanup.token}")
    private String cleanupToken;

    public TestDataSetupResponse setup(String requestToken, TestDataSetupRequest request) {
        validateToken(requestToken);

        if (request.dryRun()) {
            return buildDryRunResponse(request);
        }

        log.info("Starting test data setup (repair={})", request.repair());

        // Phase 1: Users
        log.info("Phase 1: Setting up users...");
        long createdUsers = executor.setupUsers(request.repair());
        long finalUsers = executor.countExistingUsers();
        log.info("Phase 1 complete: created={}, final={}", createdUsers, finalUsers);

        // Phase 2: Lightnings
        log.info("Phase 2: Setting up lightnings...");
        long createdLightnings = executor.setupLightnings();
        long finalLightnings = executor.countExistingLightnings();
        log.info("Phase 2 complete: created={}, final={}", createdLightnings, finalLightnings);

        // Phase 3: Participants
        log.info("Phase 3: Setting up participants...");
        long createdParticipants = executor.setupParticipants();
        long finalParticipants = executor.countExistingParticipants();
        log.info("Phase 3 complete: created={}, final={}", createdParticipants, finalParticipants);

        // Phase 4: Chat Messages
        log.info("Phase 4: Setting up chat messages...");
        long createdMessages = executor.setupChatMessages();
        long finalMessages = executor.countExistingChatMessages();
        log.info("Phase 4 complete: created={}, final={}", createdMessages, finalMessages);

        // Phase 5: lastReadChatMessageId
        log.info("Phase 5: Setting up lastReadChatMessageId...");
        executor.setupLastReadChatMessageIds();
        log.info("Phase 5 complete");

        log.info("Test data setup finished.");

        return new TestDataSetupResponse(
            false,
            request.repair(),
            executor.getTargetUserCount(),
            createdUsers,
            finalUsers,
            executor.getTargetLightningCount(),
            createdLightnings,
            finalLightnings,
            executor.getTargetParticipantCount(),
            createdParticipants,
            finalParticipants,
            executor.getTargetChatMessageCount(),
            createdMessages,
            finalMessages
        );
    }

    private TestDataSetupResponse buildDryRunResponse(TestDataSetupRequest request) {
        long existingUsers = executor.countExistingUsers();
        long existingLightnings = executor.countExistingLightnings();
        long existingParticipants = executor.countExistingParticipants();
        long existingMessages = executor.countExistingChatMessages();

        return new TestDataSetupResponse(
            true,
            request.repair(),
            executor.getTargetUserCount(),
            0,
            existingUsers,
            executor.getTargetLightningCount(),
            0,
            existingLightnings,
            executor.getTargetParticipantCount(),
            0,
            existingParticipants,
            executor.getTargetChatMessageCount(),
            0,
            existingMessages
        );
    }

    private void validateToken(String requestToken) {
        if (cleanupToken == null || cleanupToken.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Setup token is not configured.");
        }
        if (requestToken == null || requestToken.isBlank() || !cleanupToken.equals(requestToken)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid setup token.");
        }
    }
}
