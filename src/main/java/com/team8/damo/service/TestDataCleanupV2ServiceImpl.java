package com.team8.damo.service;

import com.team8.damo.controller.request.TestDataCleanupV2Request;
import com.team8.damo.controller.response.TestDataCleanupV2Response;
import com.team8.damo.repository.ChatMessageRepository;
import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.repository.LightningRepository;
import com.team8.damo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static com.team8.damo.service.TestDataSetupV2Executor.CREATOR_ID;
import static com.team8.damo.service.TestDataSetupV2Executor.V2_PREFIX;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestDataCleanupV2ServiceImpl implements TestDataCleanupV2Service {

    private static final int DEFAULT_MAX_WORKERS = 8;
    private static final int DELETE_BATCH_SIZE = 50;
    private static final int USER_START = 101;
    private static final int USER_END = 20000;

    private final LightningRepository lightningRepository;
    private final LightningParticipantRepository lightningParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final TestDataSetupV2Executor executor;

    @Value("${test-data.cleanup.token}")
    private String cleanupToken;

    @Override
    public TestDataCleanupV2Response cleanup(String requestToken, TestDataCleanupV2Request request) {
        validateToken(requestToken);

        int maxWorkers = request.maxWorkers() != null ? request.maxWorkers() : DEFAULT_MAX_WORKERS;

        List<Long> lightningIds = lightningRepository.findIdsByDescriptionPrefix(V2_PREFIX);

        if (request.dryRun()) {
            return buildDryRunResponse(lightningIds, request);
        }

        log.info("Starting V2 cleanup: {} lightnings found, maxWorkers={}", lightningIds.size(), maxWorkers);

        long deletedChatMessages = 0;
        long deletedParticipants = 0;
        long deletedLightnings = 0;
        long deletedUsers = 0;

        // Phase 1: Delete chat_messages
        if (request.deleteChatMessages() && !lightningIds.isEmpty()) {
            log.info("Cleanup Phase 1: Deleting chat messages...");
            deletedChatMessages = deleteByLightningIds(lightningIds, maxWorkers,
                "DELETE FROM chat_messages WHERE lightning_id IN (%s)",
                "test-cleanup-chat-");
            log.info("Cleanup Phase 1 complete: deleted {} chat messages", deletedChatMessages);
        }

        // Phase 2: Delete participants
        if (request.deleteParticipants() && !lightningIds.isEmpty()) {
            log.info("Cleanup Phase 2: Deleting participants...");
            deletedParticipants = deleteByLightningIds(lightningIds, maxWorkers,
                "DELETE FROM lightning_participants WHERE lightning_id IN (%s)",
                "test-cleanup-participant-");
            log.info("Cleanup Phase 2 complete: deleted {} participants", deletedParticipants);
        }

        // Phase 3: Delete lightnings
        if (request.deleteLightnings() && !lightningIds.isEmpty()) {
            log.info("Cleanup Phase 3: Deleting lightnings...");
            deletedLightnings = deleteByLightningIds(lightningIds, maxWorkers,
                "DELETE FROM lightning WHERE id IN (%s)",
                "test-cleanup-lightning-");
            log.info("Cleanup Phase 3 complete: deleted {} lightnings", deletedLightnings);
        }

        // Phase 4: Delete users (optional)
        if (request.deleteUsers()) {
            log.info("Cleanup Phase 4: Deleting users...");
            List<Long> userIds = new ArrayList<>();
            userIds.add(CREATOR_ID);
            LongStream.rangeClosed(USER_START, USER_END).forEach(userIds::add);

            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            List<List<Long>> userBatches = executor.partition(userIds, Math.max(1, userIds.size() / DELETE_BATCH_SIZE));
            for (List<Long> batch : userBatches) {
                txTemplate.executeWithoutResult(status ->
                    userRepository.deleteAllByIdInBatch(batch));
            }
            deletedUsers = userIds.size();
            log.info("Cleanup Phase 4 complete: deleted {} users", deletedUsers);
        }

        log.info("V2 cleanup finished.");

        return new TestDataCleanupV2Response(
            false,
            deletedLightnings,
            deletedParticipants,
            deletedChatMessages,
            deletedUsers
        );
    }

    private long deleteByLightningIds(List<Long> lightningIds, int maxWorkers,
                                       String sqlTemplate, String threadPrefix) {
        List<List<Long>> partitions = executor.partition(lightningIds, maxWorkers);
        AtomicLong totalDeleted = new AtomicLong(0);

        ExecutorService executorService = executor.createExecutor(maxWorkers, threadPrefix);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (List<Long> part : partitions) {
                futures.add(executorService.submit(() -> {
                    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

                    // Process in sub-batches of DELETE_BATCH_SIZE
                    for (int i = 0; i < part.size(); i += DELETE_BATCH_SIZE) {
                        int end = Math.min(i + DELETE_BATCH_SIZE, part.size());
                        List<Long> subBatch = part.subList(i, end);

                        String placeholders = subBatch.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");

                        String sql = String.format(sqlTemplate, placeholders);

                        txTemplate.executeWithoutResult(status -> {
                            int deleted = jdbcTemplate.update(sql);
                            totalDeleted.addAndGet(deleted);
                        });
                    }
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Cleanup failed: " + threadPrefix, e);
                }
            }
        } finally {
            executorService.shutdown();
        }

        return totalDeleted.get();
    }

    private TestDataCleanupV2Response buildDryRunResponse(List<Long> lightningIds, TestDataCleanupV2Request request) {
        long chatCount = 0;
        long participantCount = 0;

        if (!lightningIds.isEmpty()) {
            chatCount = chatMessageRepository.countByLightningIdIn(lightningIds);
            participantCount = lightningParticipantRepository.countByLightningIdIn(lightningIds);
        }

        long userCount = 0;
        if (request.deleteUsers()) {
            List<Long> userIds = new ArrayList<>();
            userIds.add(CREATOR_ID);
            LongStream.rangeClosed(USER_START, USER_END).forEach(userIds::add);
            userCount = userRepository.findAllById(userIds).size();
        }

        return new TestDataCleanupV2Response(
            true,
            (long) lightningIds.size(),
            participantCount,
            chatCount,
            userCount
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
}
