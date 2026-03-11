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
public class TestDataSetupExecutor {

    private static final long CREATOR_ID = 99999L;
    private static final String CREATOR_NICKNAME = "admin";
    private static final String CREATOR_EMAIL = "loadtest.creator.99999@test.com";
    private static final int USER_START = 101;
    private static final int USER_END = 1000;
    private static final int TOTAL_LIGHTNINGS = 10_000;
    private static final int OPEN_COUNT = 8_000;
    private static final int CLOSED_COUNT = 2_000;
    private static final String OPEN_PREFIX = "lt-perm-open-";
    private static final String CLOSED_PREFIX = "lt-perm-closed-";
    private static final String PERM_PREFIX = "lt-perm-";
    private static final int MAX_PARTICIPANTS = 8;
    private static final int MESSAGES_PER_ROOM = 5_000;
    private static final int BATCH_SIZE = 500;

    private final UserRepository userRepository;
    private final LightningRepository lightningRepository;
    private final LightningParticipantRepository lightningParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RestaurantRepository restaurantRepository;
    private final Snowflake snowflake;
    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    // ── Phase 1: Users ──

    @Transactional
    public long setupUsers(boolean repair) {
        List<Long> targetIds = new ArrayList<>();
        targetIds.add(CREATOR_ID);
        LongStream.rangeClosed(USER_START, USER_END).forEach(targetIds::add);

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
            saveBatch(toCreate, BATCH_SIZE, userRepository::saveAll);
        }

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
        }

        return toCreate.size();
    }

    public long countExistingUsers() {
        List<Long> targetIds = new ArrayList<>();
        targetIds.add(CREATOR_ID);
        LongStream.rangeClosed(USER_START, USER_END).forEach(targetIds::add);
        return userRepository.findAllById(targetIds).size();
    }

    // ── Phase 2: Lightnings ──

    @Transactional
    public long setupLightnings() {
        List<Restaurant> restaurants = restaurantRepository.findAll();
        if (restaurants.isEmpty()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "No restaurants found. Seed restaurant data first.");
        }

        List<Lightning> existing = lightningRepository.findAllByDescriptionPrefix(PERM_PREFIX);
        Set<String> existingDescriptions = existing.stream()
            .map(Lightning::getDescription)
            .collect(Collectors.toSet());

        List<Lightning> toCreate = new ArrayList<>();
        LocalDateTime baseDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        int restaurantIndex = 0;

        for (int i = 1; i <= TOTAL_LIGHTNINGS; i++) {
            String description;
            boolean isClosed;
            if (i <= OPEN_COUNT) {
                description = OPEN_PREFIX + String.format("%06d", i);
                isClosed = false;
            } else {
                description = CLOSED_PREFIX + String.format("%06d", i - OPEN_COUNT);
                isClosed = true;
            }

            if (existingDescriptions.contains(description)) {
                continue;
            }

            Lightning lightning = Lightning.builder()
                .id(snowflake.nextId())
                .restaurantId(restaurants.get(restaurantIndex % restaurants.size()).getId())
                .maxParticipants(MAX_PARTICIPANTS)
                .description(description)
                .lightningDate(baseDate.plusMinutes(i))
                .build();

            if (isClosed) {
                lightning.close();
            }

            toCreate.add(lightning);
            restaurantIndex++;
        }

        if (!toCreate.isEmpty()) {
            saveBatch(toCreate, BATCH_SIZE, lightningRepository::saveAll);
        }

        return toCreate.size();
    }

    public long countExistingLightnings() {
        return lightningRepository.findAllByDescriptionPrefix(PERM_PREFIX).size();
    }

    // ── Phase 3: Participants ──

    @Transactional
    public long setupParticipants() {
        List<Lightning> allLightnings = lightningRepository.findAllByDescriptionPrefix(PERM_PREFIX);
        if (allLightnings.isEmpty()) {
            return 0;
        }

        List<Long> allIds = allLightnings.stream().map(Lightning::getId).toList();
        Map<Long, Lightning> lightningMap = allLightnings.stream()
            .collect(Collectors.toMap(Lightning::getId, l -> l));

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

        List<Lightning> closedLightnings = allLightnings.stream()
            .filter(Lightning::isClosed)
            .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
            .toList();

        List<LightningParticipant> toCreate = new ArrayList<>();

        // Add leaders for all lightnings that don't have one
        for (Lightning lightning : allLightnings) {
            if (!lightningsWithLeader.contains(lightning.getId())) {
                toCreate.add(LightningParticipant.createLeader(snowflake.nextId(), lightning, creator));
            }
        }

        // Add participants only for CLOSED lightnings
        int userIndex = 0;
        for (int i = 0; i < closedLightnings.size(); i++) {
            Lightning lightning = closedLightnings.get(i);
            long existingCount = countMap.getOrDefault(lightning.getId(), 0L);

            int targetCount;
            double ratio = (double) i / closedLightnings.size();
            if (ratio < 0.4) {
                targetCount = 8; // 40%: 8 participants
            } else if (ratio < 0.8) {
                targetCount = 5; // 40%: 5 participants
            } else {
                targetCount = 3; // 20%: 3 participants
            }

            long needed = targetCount - existingCount;
            if (needed <= 0) {
                continue;
            }

            // Leader counts as 1, so we need (needed) more normal participants
            // But existingCount already includes leader if present
            for (long j = 0; j < needed; j++) {
                User user = normalUsers.get(userIndex % normalUsers.size());
                toCreate.add(LightningParticipant.createParticipant(snowflake.nextId(), lightning, user));
                userIndex++;
            }
        }

        if (!toCreate.isEmpty()) {
            saveBatch(toCreate, 1000, lightningParticipantRepository::saveAll);
        }

        return toCreate.size();
    }

    public long countExistingParticipants() {
        List<Long> ids = lightningRepository.findIdsByDescriptionPrefix(PERM_PREFIX);
        if (ids.isEmpty()) return 0;
        return lightningParticipantRepository.countByLightningIdIn(ids);
    }

    // ── Phase 4: Chat Messages (멀티스레드 병렬 처리) ──

    private static final int THREAD_COUNT = 4;

    public long setupChatMessages() {
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

        // 작업이 필요한 방만 필터링
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

        // 방 목록을 스레드 수만큼 균등 분할
        List<List<Lightning>> partitions = partition(roomsToProcess, THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (List<Lightning> partition : partitions) {
                futures.add(executor.submit(() ->
                    processPartition(partition, countMap, participantMap, totalCreated, processedCount, totalRooms)
                ));
            }

            // 모든 스레드 완료 대기, 예외 전파
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException("Chat message setup failed", e);
                }
            }
        } finally {
            executor.shutdown();
        }

        return totalCreated.get();
    }

    private void processPartition(
        List<Lightning> lightnings,
        Map<Long, Long> countMap,
        Map<Long, List<LightningParticipant>> participantMap,
        AtomicLong totalCreated,
        AtomicInteger processedCount,
        int totalRooms
    ) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        String sql = "INSERT INTO chat_messages (id, lightning_id, users_id, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";

        for (Lightning lightning : lightnings) {
            long existingCount = countMap.getOrDefault(lightning.getId(), 0L);
            long needed = MESSAGES_PER_ROOM - existingCount;

            List<LightningParticipant> participants = participantMap.get(lightning.getId());
            long lightningId = lightning.getId();

            txTemplate.executeWithoutResult(status -> {
                List<Object[]> batchArgs = new ArrayList<>(1000);
                LocalDateTime now = LocalDateTime.now();

                for (long seq = existingCount + 1; seq <= MESSAGES_PER_ROOM; seq++) {
                    LightningParticipant sender = participants.get((int) (seq % participants.size()));
                    String content = "lt-perm message room=" + lightningId + " seq=" + seq;

                    batchArgs.add(new Object[]{
                        snowflake.nextId(),
                        lightningId,
                        sender.getUser().getId(),
                        content,
                        now,
                        now
                    });

                    if (batchArgs.size() >= 1000) {
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

            if (processed % 50 == 0) {
                log.info("Chat messages progress: {}/{} rooms processed, {} messages created",
                    processed, totalRooms, totalCreated.get());
            }
        }
    }

    private <T> List<List<T>> partition(List<T> list, int partitionCount) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < partitionCount; i++) {
            partitions.add(new ArrayList<>());
        }
        for (int i = 0; i < list.size(); i++) {
            partitions.get(i % partitionCount).add(list.get(i));
        }
        return partitions;
    }

    public long countExistingChatMessages() {
        List<Long> closedIds = lightningRepository.findIdsByDescriptionPrefix(CLOSED_PREFIX);
        if (closedIds.isEmpty()) return 0;
        return chatMessageRepository.countByLightningIdIn(closedIds);
    }

    // ── Phase 5: lastReadChatMessageId ──

    @Transactional
    public void setupLastReadChatMessageIds() {
        List<Lightning> closedLightnings = lightningRepository.findAllByDescriptionPrefix(CLOSED_PREFIX);
        if (closedLightnings.isEmpty()) {
            return;
        }

        List<Long> closedIds = closedLightnings.stream().map(Lightning::getId).toList();
        List<LightningParticipant> allParticipants = lightningParticipantRepository.findAllByLightningIdIn(closedIds);
        Map<Long, List<LightningParticipant>> participantMap = allParticipants.stream()
            .collect(Collectors.groupingBy(lp -> lp.getLightning().getId()));

        for (Lightning lightning : closedLightnings) {
            Long latestMessageId = chatMessageRepository.findLatestMessageId(lightning.getId());
            if (latestMessageId == null) {
                continue;
            }

            List<LightningParticipant> participants = participantMap.getOrDefault(lightning.getId(), List.of());
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

    // ── Utility ──

    private <T> void saveBatch(List<T> items, int batchSize, java.util.function.Consumer<List<T>> saveAll) {
        for (int i = 0; i < items.size(); i += batchSize) {
            int end = Math.min(i + batchSize, items.size());
            saveAll.accept(items.subList(i, end));
        }
    }

    // ── Target count helpers ──

    public long getTargetUserCount() {
        return 901; // creator + 900 normal users
    }

    public long getTargetLightningCount() {
        return TOTAL_LIGHTNINGS;
    }

    public long getTargetParticipantCount() {
        // All 10,000 lightnings have 1 leader = 10,000
        // CLOSED: 800 rooms * 8 + 800 * 5 + 400 * 3 = 6400 + 4000 + 1200 = 11,600
        // But leader is already counted, so additional participants for CLOSED:
        // 800*(8-1) + 800*(5-1) + 400*(3-1) = 5600 + 3200 + 800 = 9600
        // Total = 10,000 leaders + 9,600 participants = 19,600
        return 19_600;
    }

    public long getTargetChatMessageCount() {
        return (long) CLOSED_COUNT * MESSAGES_PER_ROOM; // 2000 * 5000 = 10,000,000
    }
}
