package com.team8.damo.service;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.AgeGroup;
import com.team8.damo.entity.enumeration.Gender;
import com.team8.damo.entity.enumeration.OnboardingStep;
import com.team8.damo.repository.ChatMessageRepository;
import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.repository.LightningRepository;
import com.team8.damo.repository.RestaurantRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataSetupV2Executor {

    // ── Constants ──

    static final long CREATOR_ID = 99999L;
    private static final String CREATOR_NICKNAME = "admin";
    private static final String CREATOR_EMAIL = "admin@test.com";
    private static final int USER_START = 101;
    private static final int USER_END = 20000;

    private static final int OPEN_COUNT = 40_000;
    private static final int CLOSED_COUNT = 10_000;
    private static final String OPEN_PREFIX = "lt-v2-open-";
    private static final String CLOSED_PREFIX = "lt-v2-closed-";
    static final String V2_PREFIX = "lt-v2-";
    private static final int MAX_PARTICIPANTS = 8;
    private static final int MESSAGES_PER_ROOM = 5_000;
    private static final int SAVE_BATCH_SIZE = 500;

    private final UserRepository userRepository;
    private final LightningRepository lightningRepository;
    private final LightningParticipantRepository lightningParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RestaurantRepository restaurantRepository;
    private final Snowflake snowflake;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    // ══════════════════════════════════════════════
    // Phase 1: USERS (단일 스레드)
    // ══════════════════════════════════════════════

    @Transactional
    public long[] setupUsers(boolean repair) {
        List<Long> targetIds = buildTargetUserIds();

        List<User> existingUsers = userRepository.findAllById(targetIds);
        Set<Long> existingIds = existingUsers.stream()
            .map(User::getId)
            .collect(Collectors.toSet());

        List<User> toCreate = new ArrayList<>();
        for (Long id : targetIds) {
            if (!existingIds.contains(id)) {
                if (id == CREATOR_ID) {
                    toCreate.add(new User(id, CREATOR_EMAIL, id));
                } else {
                    toCreate.add(new User(id, "user" + id + "@test.com", id));
                }
            }
        }

        if (!toCreate.isEmpty()) {
            saveBatch(toCreate, SAVE_BATCH_SIZE, userRepository::saveAll);
        }

        long updatedCount = 0;
        if (repair) {
            List<User> allUsers = userRepository.findAllById(targetIds);
            for (User user : allUsers) {
                if (user.getId() == CREATOR_ID) {
                    user.updateBasic(CREATOR_NICKNAME, Gender.MALE, AgeGroup.TWENTIES);
                } else {
                    user.updateBasic("사용자" + user.getId(), Gender.MALE, AgeGroup.TWENTIES);
                }
                user.updateOnboardingStep(OnboardingStep.DONE);
            }
            updatedCount = allUsers.size();
        }

        return new long[]{toCreate.size(), updatedCount};
    }

    public long countExistingUsers() {
        return userRepository.findAllById(buildTargetUserIds()).size();
    }

    private List<Long> buildTargetUserIds() {
        List<Long> ids = new ArrayList<>();
        ids.add(CREATOR_ID);
        LongStream.rangeClosed(USER_START, USER_END).forEach(ids::add);
        return ids;
    }

    // ══════════════════════════════════════════════
    // Phase 2: LIGHTNINGS (멀티스레드)
    // ══════════════════════════════════════════════

    public long[] setupLightnings(int maxWorkers) {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        if (restaurants.isEmpty()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "No restaurants found. Seed restaurant data first.");
        }

        List<Lightning> existing = lightningRepository.findAllByDescriptionPrefix(V2_PREFIX);
        Set<String> existingDescriptions = existing.stream()
            .map(Lightning::getDescription)
            .collect(Collectors.toSet());

        List<Object[]> toCreateData = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.of(2026, 6, 1, 12, 0);

        for (int i = 1; i <= OPEN_COUNT; i++) {
            String description = OPEN_PREFIX + String.format("%06d", i);
            if (!existingDescriptions.contains(description)) {
                toCreateData.add(new Object[]{description, false, i});
            }
        }
        long createdOpen = toCreateData.size();

        int closedStart = toCreateData.size();
        for (int i = 1; i <= CLOSED_COUNT; i++) {
            String description = CLOSED_PREFIX + String.format("%06d", i);
            if (!existingDescriptions.contains(description)) {
                toCreateData.add(new Object[]{description, true, OPEN_COUNT + i});
            }
        }
        long createdClosed = toCreateData.size() - closedStart;

        if (toCreateData.isEmpty()) {
            return new long[]{0, 0};
        }

        List<List<Object[]>> partitions = partition(toCreateData, maxWorkers);
        ExecutorService executor = createExecutor(maxWorkers, "test-setup-lightning-");
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (List<Object[]> part : partitions) {
                futures.add(executor.submit(() -> {
                    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                    List<Lightning> batch = new ArrayList<>(SAVE_BATCH_SIZE);
                    int restIdx = 0;

                    for (Object[] data : part) {
                        String desc = (String) data[0];
                        boolean isClosed = (boolean) data[1];
                        int seq = (int) data[2];

                        Lightning lightning = Lightning.builder()
                            .id(snowflake.nextId())
                            .restaurantId(restaurants.get(restIdx % restaurants.size()).getId())
                            .maxParticipants(MAX_PARTICIPANTS)
                            .description(desc)
                            .lightningDate(baseDate.plusMinutes(seq))
                            .build();

                        if (isClosed) {
                            lightning.close();
                        }

                        batch.add(lightning);
                        restIdx++;

                        if (batch.size() >= SAVE_BATCH_SIZE) {
                            List<Lightning> toSave = new ArrayList<>(batch);
                            txTemplate.executeWithoutResult(status -> lightningRepository.saveAll(toSave));
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        List<Lightning> toSave = new ArrayList<>(batch);
                        txTemplate.executeWithoutResult(status -> lightningRepository.saveAll(toSave));
                    }
                }));
            }
            waitForAll(futures, "Lightning setup");
        } finally {
            executor.shutdown();
        }

        return new long[]{createdOpen, createdClosed};
    }

    public long countExistingOpenLightnings() {
        return lightningRepository.findAllByDescriptionPrefix(OPEN_PREFIX).size();
    }

    public long countExistingClosedLightnings() {
        return lightningRepository.findAllByDescriptionPrefix(CLOSED_PREFIX).size();
    }

    // ══════════════════════════════════════════════
    // Phase 3: PARTICIPANTS (멀티스레드)
    // ══════════════════════════════════════════════

    public long setupParticipants(int maxWorkers) {
        List<Lightning> allLightnings = lightningRepository.findAllByDescriptionPrefix(V2_PREFIX);
        if (allLightnings.isEmpty()) {
            return 0;
        }

        List<Long> allIds = allLightnings.stream().map(Lightning::getId).toList();

        Set<Long> lightningsWithLeader = lightningParticipantRepository
            .findLeadersByLightningIds(allIds).stream()
            .map(lp -> lp.getLightning().getId())
            .collect(Collectors.toSet());

        List<Object[]> participantCounts = lightningParticipantRepository.countByLightningIds(allIds);
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : participantCounts) {
            countMap.put((Long) row[0], (Long) row[1]);
        }

        User creator = userRepository.findById(CREATOR_ID)
            .orElseThrow(() -> new ResponseStatusException(SERVICE_UNAVAILABLE, "Creator user not found."));

        List<User> normalUsers = userRepository.findAllById(
            LongStream.rangeClosed(USER_START, USER_END).boxed().toList()
        );
        if (normalUsers.isEmpty()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Normal users not found.");
        }

        // description으로 정렬하여 순번 기반 분포 결정
        allLightnings.sort((a, b) -> a.getDescription().compareTo(b.getDescription()));

        // 방별 작업 데이터 구성: [lightning, targetCount, hasLeader]
        List<Object[]> roomTasks = new ArrayList<>();
        for (Lightning lightning : allLightnings) {
            int targetCount = getTargetParticipantCount(lightning.getDescription());
            boolean hasLeader = lightningsWithLeader.contains(lightning.getId());
            long existingCount = countMap.getOrDefault(lightning.getId(), 0L);

            if (!hasLeader || existingCount < targetCount) {
                roomTasks.add(new Object[]{lightning, targetCount, hasLeader, existingCount});
            }
        }

        if (roomTasks.isEmpty()) {
            return 0;
        }

        List<List<Object[]>> partitions = partition(roomTasks, maxWorkers);
        AtomicLong totalCreated = new AtomicLong(0);

        ExecutorService executor = createExecutor(maxWorkers, "test-setup-participant-");
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int workerIdx = 0; workerIdx < partitions.size(); workerIdx++) {
                List<Object[]> part = partitions.get(workerIdx);
                int userOffset = workerIdx * (normalUsers.size() / maxWorkers);

                futures.add(executor.submit(() -> {
                    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                    List<LightningParticipant> batch = new ArrayList<>(1000);
                    int uIdx = userOffset;

                    for (Object[] task : part) {
                        Lightning lightning = (Lightning) task[0];
                        int targetCount = (int) task[1];
                        boolean hasLeader = (boolean) task[2];
                        long existingCount = (long) task[3];

                        if (!hasLeader) {
                            batch.add(LightningParticipant.createLeader(snowflake.nextId(), lightning, creator));
                        }

                        long needed = targetCount - existingCount;
                        if (hasLeader) {
                            // leader already counted in existingCount
                        } else {
                            // leader just added, so we need (targetCount - 1) more participants beyond leader
                            needed = targetCount - existingCount - 1;
                        }

                        for (long j = 0; j < needed; j++) {
                            User user = normalUsers.get(uIdx % normalUsers.size());
                            batch.add(LightningParticipant.createParticipant(snowflake.nextId(), lightning, user));
                            uIdx++;
                        }

                        if (batch.size() >= 1000) {
                            List<LightningParticipant> toSave = new ArrayList<>(batch);
                            txTemplate.executeWithoutResult(status ->
                                lightningParticipantRepository.saveAll(toSave));
                            totalCreated.addAndGet(toSave.size());
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        List<LightningParticipant> toSave = new ArrayList<>(batch);
                        txTemplate.executeWithoutResult(status ->
                            lightningParticipantRepository.saveAll(toSave));
                        totalCreated.addAndGet(toSave.size());
                    }
                }));
            }
            waitForAll(futures, "Participant setup");
        } finally {
            executor.shutdown();
        }

        return totalCreated.get();
    }

    private int getTargetParticipantCount(String description) {
        if (description.startsWith(OPEN_PREFIX)) {
            int seq = Integer.parseInt(description.substring(OPEN_PREFIX.length()));
            if (seq <= 5000) return 1;
            if (seq <= 10000) return 4;
            if (seq <= 30000) return 7;
            return 8; // 30001~40000
        } else {
            // CLOSED
            int seq = Integer.parseInt(description.substring(CLOSED_PREFIX.length()));
            if (seq <= 5000) return 8;
            if (seq <= 8000) return 6;
            return 2; // 8001~10000
        }
    }

    public long countExistingParticipants() {
        List<Long> ids = lightningRepository.findIdsByDescriptionPrefix(V2_PREFIX);
        if (ids.isEmpty()) return 0;
        return lightningParticipantRepository.countByLightningIdIn(ids);
    }

    // ══════════════════════════════════════════════
    // Phase 4: CHAT_MESSAGES (멀티스레드, JDBC batch)
    // ══════════════════════════════════════════════

    public long setupChatMessages(int maxWorkers, int messageBatchSize) {
        List<Lightning> closedLightnings = lightningRepository.findAllByDescriptionPrefix(CLOSED_PREFIX);
        if (closedLightnings.isEmpty()) {
            return 0;
        }

        List<Long> closedIds = closedLightnings.stream().map(Lightning::getId).toList();
        List<Object[]> msgCounts = chatMessageRepository.countByLightningIds(closedIds);
        Map<Long, Long> countMap = new HashMap<>();
        for (Object[] row : msgCounts) {
            countMap.put((Long) row[0], (Long) row[1]);
        }

        List<LightningParticipant> allParticipants = lightningParticipantRepository.findAllByLightningIdIn(closedIds);
        Map<Long, List<LightningParticipant>> participantMap = allParticipants.stream()
            .collect(Collectors.groupingBy(lp -> lp.getLightning().getId()));

        List<Lightning> roomsToProcess = closedLightnings.stream()
            .filter(l -> {
                long existing = countMap.getOrDefault(l.getId(), 0L);
                List<LightningParticipant> participants = participantMap.getOrDefault(l.getId(), List.of());
                return existing < MESSAGES_PER_ROOM && !participants.isEmpty();
            })
            .toList();

        if (roomsToProcess.isEmpty()) {
            return 0;
        }

        AtomicLong totalCreated = new AtomicLong(0);
        AtomicInteger processedCount = new AtomicInteger(0);
        int totalRooms = roomsToProcess.size();

        List<List<Lightning>> partitions = partition(roomsToProcess, maxWorkers);
        ExecutorService executor = createExecutor(maxWorkers, "test-setup-chat-");
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (List<Lightning> part : partitions) {
                futures.add(executor.submit(() ->
                    processChatPartition(part, countMap, participantMap, totalCreated,
                        processedCount, totalRooms, messageBatchSize)
                ));
            }
            waitForAll(futures, "Chat message setup");
        } finally {
            executor.shutdown();
        }

        return totalCreated.get();
    }

    private void processChatPartition(
        List<Lightning> lightnings,
        Map<Long, Long> countMap,
        Map<Long, List<LightningParticipant>> participantMap,
        AtomicLong totalCreated,
        AtomicInteger processedCount,
        int totalRooms,
        int messageBatchSize
    ) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        String sql = "INSERT INTO chat_messages (id, lightning_id, users_id, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";

        for (Lightning lightning : lightnings) {
            long existingCount = countMap.getOrDefault(lightning.getId(), 0L);
            long needed = MESSAGES_PER_ROOM - existingCount;

            List<LightningParticipant> participants = participantMap.get(lightning.getId());
            long lightningId = lightning.getId();

            txTemplate.executeWithoutResult(status -> {
                List<Object[]> batchArgs = new ArrayList<>(messageBatchSize);
                LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 0, 0);

                for (long seq = existingCount + 1; seq <= MESSAGES_PER_ROOM; seq++) {
                    LightningParticipant sender = participants.get((int) (seq % participants.size()));
                    String content = "lt-v2 message room=" + lightningId + " seq=" + seq;
                    LocalDateTime msgTime = baseTime.plusSeconds(seq * 2);

                    batchArgs.add(new Object[]{
                        snowflake.nextId(),
                        lightningId,
                        sender.getUser().getId(),
                        content,
                        msgTime,
                        msgTime
                    });

                    if (batchArgs.size() >= messageBatchSize) {
                        jdbcTemplate.batchUpdate(sql, batchArgs);
                        batchArgs.clear();
                    }
                }

                if (!batchArgs.isEmpty()) {
                    jdbcTemplate.batchUpdate(sql, batchArgs);
                }
            });

            totalCreated.addAndGet(needed);
            int processed = processedCount.incrementAndGet();

            if (processed % 100 == 0) {
                log.info("Chat messages progress: {}/{} rooms processed, {} messages created",
                    processed, totalRooms, totalCreated.get());
            }
        }
    }

    public long countExistingChatMessages() {
        List<Long> closedIds = lightningRepository.findIdsByDescriptionPrefix(CLOSED_PREFIX);
        if (closedIds.isEmpty()) return 0;
        return chatMessageRepository.countByLightningIdIn(closedIds);
    }

    // ══════════════════════════════════════════════
    // Phase 5: LAST_READ (멀티스레드)
    // ══════════════════════════════════════════════

    public void setupLastReadChatMessageIds(int maxWorkers) {
        List<Lightning> closedLightnings = lightningRepository.findAllByDescriptionPrefix(CLOSED_PREFIX);
        if (closedLightnings.isEmpty()) {
            return;
        }

        List<List<Lightning>> partitions = partition(closedLightnings, maxWorkers);
        ExecutorService executor = createExecutor(maxWorkers, "test-setup-lastread-");
        try {
            List<Future<?>> futures = new ArrayList<>();
            AtomicInteger processedCount = new AtomicInteger(0);
            int totalRooms = closedLightnings.size();

            for (List<Lightning> part : partitions) {
                futures.add(executor.submit(() -> {
                    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                    List<Lightning> batch = new ArrayList<>(100);

                    for (Lightning lightning : part) {
                        batch.add(lightning);

                        if (batch.size() >= 100) {
                            List<Lightning> toProcess = new ArrayList<>(batch);
                            txTemplate.executeWithoutResult(status ->
                                processLastReadBatch(toProcess));
                            int processed = processedCount.addAndGet(toProcess.size());
                            if (processed % 1000 == 0) {
                                log.info("LastRead progress: {}/{} rooms processed", processed, totalRooms);
                            }
                            batch.clear();
                        }
                    }

                    if (!batch.isEmpty()) {
                        List<Lightning> toProcess = new ArrayList<>(batch);
                        txTemplate.executeWithoutResult(status ->
                            processLastReadBatch(toProcess));
                        processedCount.addAndGet(toProcess.size());
                    }
                }));
            }
            waitForAll(futures, "LastRead setup");
        } finally {
            executor.shutdown();
        }
    }

    private void processLastReadBatch(List<Lightning> lightnings) {
        for (Lightning lightning : lightnings) {
            Long latestMessageId = chatMessageRepository.findLatestMessageId(lightning.getId());
            if (latestMessageId == null) {
                continue;
            }

            List<LightningParticipant> participants =
                lightningParticipantRepository.findAllByLightningIdIn(List.of(lightning.getId()));
            long seventyPercentId = (long) (latestMessageId * 0.7);

            for (int i = 0; i < participants.size(); i++) {
                LightningParticipant lp = participants.get(i);
                double ratio = (double) i / participants.size();

                long lastReadId;
                if (ratio < 0.3) {
                    lastReadId = 0L;
                } else if (ratio < 0.7) {
                    lastReadId = seventyPercentId;
                } else {
                    lastReadId = latestMessageId;
                }

                lp.updateLastReadChatMessageId(lastReadId);
            }
        }
    }

    // ══════════════════════════════════════════════
    // Target counts
    // ══════════════════════════════════════════════

    public long getTargetUserCount() {
        return 1 + (USER_END - USER_START + 1); // creator + normal users = 19,901
    }

    public long getTargetOpenLightningCount() {
        return OPEN_COUNT;
    }

    public long getTargetClosedLightningCount() {
        return CLOSED_COUNT;
    }

    public long getTargetParticipantCount() {
        // OPEN: 5000*1 + 5000*4 + 20000*7 + 10000*8 = 5000+20000+140000+80000 = 245,000
        // CLOSED: 5000*8 + 3000*6 + 2000*2 = 40000+18000+4000 = 62,000
        return 307_000;
    }

    public long getTargetChatMessageCount() {
        return (long) CLOSED_COUNT * MESSAGES_PER_ROOM; // 10,000 * 5,000 = 50,000,000
    }

    // ══════════════════════════════════════════════
    // Utilities
    // ══════════════════════════════════════════════

    ExecutorService createExecutor(int maxWorkers, String namePrefix) {
        return Executors.newFixedThreadPool(maxWorkers, r -> {
            Thread t = new Thread(r);
            t.setName(namePrefix + t.threadId());
            return t;
        });
    }

    <T> List<List<T>> partition(List<T> list, int partitionCount) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < partitionCount; i++) {
            partitions.add(new ArrayList<>());
        }
        for (int i = 0; i < list.size(); i++) {
            partitions.get(i % partitionCount).add(list.get(i));
        }
        return partitions;
    }

    private <T> void saveBatch(List<T> items, int batchSize, java.util.function.Consumer<List<T>> saveAll) {
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            saveAll.accept(items.subList(i, end));
        }
    }

    private void waitForAll(List<Future<?>> futures, String phaseName) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                throw new RuntimeException(phaseName + " failed", e);
            }
        }
    }
}
