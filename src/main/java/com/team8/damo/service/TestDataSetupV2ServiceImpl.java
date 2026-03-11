package com.team8.damo.service;

import com.team8.damo.controller.request.TestDataSetupV2Request;
import com.team8.damo.controller.response.TestDataSetupV2Response;
import com.team8.damo.entity.enumeration.SetupPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataSetupV2ServiceImpl implements TestDataSetupV2Service {

    private static final int DEFAULT_MAX_WORKERS = 8;
    private static final int DEFAULT_MESSAGE_BATCH_SIZE = 2000;

    private final TestDataSetupV2Executor executor;

    @Value("${test-data.cleanup.token}")
    private String cleanupToken;

    @Override
    public TestDataSetupV2Response setup(String requestToken, TestDataSetupV2Request request) {
        validateToken(requestToken);

        int maxWorkers = request.maxWorkers() != null ? request.maxWorkers() : DEFAULT_MAX_WORKERS;
        int messageBatchSize = request.messageBatchSize() != null ? request.messageBatchSize() : DEFAULT_MESSAGE_BATCH_SIZE;

        Set<SetupPhase> phases = request.phases() != null && !request.phases().isEmpty()
            ? Set.copyOf(request.phases())
            : Arrays.stream(SetupPhase.values()).collect(Collectors.toSet());

        if (request.dryRun()) {
            return buildDryRunResponse(request);
        }

        log.info("Starting V2 test data setup (repair={}, phases={}, maxWorkers={}, messageBatchSize={})",
            request.repair(), phases, maxWorkers, messageBatchSize);

        List<String> completedPhases = new ArrayList<>();
        long createdUserCount = 0;
        long updatedUserCount = 0;
        long createdOpenCount = 0;
        long createdClosedCount = 0;
        long createdParticipantCount = 0;
        long createdChatMessageCount = 0;

        // Phase 1: USERS
        if (phases.contains(SetupPhase.USERS)) {
            log.info("Phase 1: Setting up users...");
            long[] result = executor.setupUsers(request.repair());
            createdUserCount = result[0];
            updatedUserCount = result[1];
            log.info("Phase 1 complete: created={}, updated={}", createdUserCount, updatedUserCount);
            completedPhases.add("USERS");
        }

        // Phase 2: LIGHTNINGS
        if (phases.contains(SetupPhase.LIGHTNINGS)) {
            log.info("Phase 2: Setting up lightnings...");
            long[] result = executor.setupLightnings(maxWorkers);
            createdOpenCount = result[0];
            createdClosedCount = result[1];
            log.info("Phase 2 complete: createdOpen={}, createdClosed={}", createdOpenCount, createdClosedCount);
            completedPhases.add("LIGHTNINGS");
        }

        // Phase 3: PARTICIPANTS
        if (phases.contains(SetupPhase.PARTICIPANTS)) {
            log.info("Phase 3: Setting up participants...");
            createdParticipantCount = executor.setupParticipants(maxWorkers);
            log.info("Phase 3 complete: created={}", createdParticipantCount);
            completedPhases.add("PARTICIPANTS");
        }

        // Phase 4: CHAT_MESSAGES
        if (phases.contains(SetupPhase.CHAT_MESSAGES)) {
            log.info("Phase 4: Setting up chat messages...");
            createdChatMessageCount = executor.setupChatMessages(maxWorkers, messageBatchSize);
            log.info("Phase 4 complete: created={}", createdChatMessageCount);
            completedPhases.add("CHAT_MESSAGES");
        }

        // Phase 5: LAST_READ
        if (phases.contains(SetupPhase.LAST_READ)) {
            log.info("Phase 5: Setting up lastReadChatMessageId...");
            executor.setupLastReadChatMessageIds(maxWorkers);
            log.info("Phase 5 complete");
            completedPhases.add("LAST_READ");
        }

        log.info("V2 test data setup finished. completedPhases={}", completedPhases);

        return new TestDataSetupV2Response(
            false,
            request.repair(),
            executor.getTargetUserCount(),
            executor.getTargetOpenLightningCount(),
            executor.getTargetClosedLightningCount(),
            executor.getTargetParticipantCount(),
            executor.getTargetChatMessageCount(),
            createdUserCount,
            updatedUserCount,
            createdOpenCount,
            createdClosedCount,
            createdParticipantCount,
            createdChatMessageCount,
            executor.countExistingUsers(),
            executor.countExistingOpenLightnings(),
            executor.countExistingClosedLightnings(),
            executor.countExistingParticipants(),
            executor.countExistingChatMessages(),
            completedPhases
        );
    }

    private TestDataSetupV2Response buildDryRunResponse(TestDataSetupV2Request request) {
        return new TestDataSetupV2Response(
            true,
            request.repair(),
            executor.getTargetUserCount(),
            executor.getTargetOpenLightningCount(),
            executor.getTargetClosedLightningCount(),
            executor.getTargetParticipantCount(),
            executor.getTargetChatMessageCount(),
            0, 0, 0, 0, 0, 0,
            executor.countExistingUsers(),
            executor.countExistingOpenLightnings(),
            executor.countExistingClosedLightnings(),
            executor.countExistingParticipants(),
            executor.countExistingChatMessages(),
            List.of()
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
