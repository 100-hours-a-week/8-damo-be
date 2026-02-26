package com.team8.damo.service;

import com.team8.damo.entity.User;
import com.team8.damo.event.payload.RecommendationStreamingEventPayload;
import com.team8.damo.redis.key.RedisKeyPrefix;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.response.RecommendationStreamingResponse;
import com.team8.damo.service.response.SseEventType;
import com.team8.damo.util.DataSerializer;
import com.team8.damo.util.Snowflake;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.team8.damo.service.response.SseEventType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private static final long TIMEOUT = 10 * 60 * 1000L;
    private static final long RECONNECTION_TIMEOUT = 1000L;
    private static final long HEARTBEAT_INTERVAL = 30L;

    private final Map<Long, Map<Long, SseEmitter>> sseStreamingMap = new ConcurrentHashMap<>();

    private final Snowflake snowflake;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public SseEmitter subscribe(Long userId, Long diningId) {
        SseEmitter newEmitter = new SseEmitter(TIMEOUT);

        newEmitter.onCompletion(() -> removeIfSame(diningId, userId, newEmitter, "completion", null));

        newEmitter.onTimeout(() -> {
            removeIfSame(diningId, userId, newEmitter, "timeout", null);
            safeComplete(newEmitter);
        });

        newEmitter.onError(ex -> removeIfSame(diningId, userId, newEmitter, "error", ex));

        SseEmitter oldEmitter = sseStreamingMap
            .computeIfAbsent(diningId, k -> new ConcurrentHashMap<>())
            .put(userId, newEmitter);

        if (oldEmitter != null && oldEmitter != newEmitter) {
            safeComplete(oldEmitter);
        }

        boolean connected = sendEvent(diningId, userId, newEmitter, CONNECTED, "SSE connected");
        if (!connected) {
            removeIfSame(diningId, userId, newEmitter, "initial-send-error", null);
            safeComplete(newEmitter);
        }
        return newEmitter;
    }

    private void removeIfSame(Long diningId, Long userId, SseEmitter expected, String reason, Throwable error) {
        AtomicReference<SseEmitter> removedRef = new AtomicReference<>();

        sseStreamingMap.computeIfPresent(diningId, (id, emitters) -> {
            emitters.compute(userId, (uid, current) -> {
                if (current == expected) {
                    removedRef.set(current);
                    return null;
                }
                return current;
            });
            return emitters.isEmpty() ? null : emitters;
        });

        if (removedRef.get() != null) {
            if (error == null) {
                log.info(
                    "[SseEmitterService.removeIfSame] removed. diningId={}, userId={}, reason={}",
                    diningId, userId, reason
                );
            } else {
                log.warn(
                    "[SseEmitterService.removeIfSame] removed. diningId={}, userId={}, reason={}",
                    diningId, userId, reason, error
                );
            }
        }
    }

    private void safeComplete(SseEmitter emitter) {
        if (emitter == null) {
            return;
        }

        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // 이미 완료된 emitter
        } catch (Exception e) {
            log.debug("[SseEmitterService.safeComplete] complete failed", e);
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, Throwable t) {
        if (emitter == null) {
            return;
        }

        try {
            emitter.completeWithError(t);
        } catch (IllegalStateException ignored) {
            // 이미 완료된 emitter
        } catch (Exception e) {
            log.debug("[SseEmitterService.safeCompleteWithError] completeWithError failed", e);
        }
    }

    public void broadcast(Long diningId, String eventName, RecommendationStreamingEventPayload data) {
        User user = userRepository.findById(data.userId()).orElse(null);

        RecommendationStreamingResponse streamingResponse = RecommendationStreamingResponse.builder()
            .eventId(snowflake.nextId())
            .userId(data.userId())
            .nickname(user == null ? "익명" : user.getNickname())
            .content(data.content())
            .createdAt(LocalDateTime.now())
            .build();

        String key = RedisKeyPrefix.DINING_RECOMMENDATION_STREAMING.key(diningId);
        redisTemplate.opsForList().rightPush(key, DataSerializer.serialize(streamingResponse));
        redisTemplate.expire(key, Duration.ofMinutes(30));

        sseStreamingMap.get(diningId)
            .forEach((userId, emitter) ->
                sendEvent(diningId, userId, emitter, STREAMING, streamingResponse)
            );
    }

    public void completeAll(Long diningId) {
        Map<Long, SseEmitter> emitters = sseStreamingMap.remove(diningId);
        if (emitters == null) {
            return;
        }

        emitters.forEach((userId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn(
                    "[SseEmitterService.completeAll] complete failed. diningId={}, userId={}",
                    diningId, userId, e
                );
            }
        });
    }

    private boolean sendEvent(Long diningId, Long userId, SseEmitter emitter, SseEventType eventType, Object data) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventType.getValue())
                    .reconnectTime(RECONNECTION_TIMEOUT)
                    .data(data)
            );
            log.info("[SseEmitterService.sendEvent] data={}", data);
            return true;
        } catch (Exception e) {
            removeIfSame(diningId, userId, emitter, "send-error", e);
            safeCompleteWithError(emitter, e);
            return false;
        }
    }

    private final ScheduledExecutorService heartbeatScheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });

    @PostConstruct
    private void startHeartbeat() {
        heartbeatScheduler.scheduleWithFixedDelay(
            this::runHeartbeatSafely,
            HEARTBEAT_INTERVAL,  // 첫 실행까지 대기 시간
            HEARTBEAT_INTERVAL,  // 반복 주기
            TimeUnit.SECONDS
        );
    }

    @PreDestroy
    private void stopHeartbeat() {
        heartbeatScheduler.shutdownNow();
    }

    private void runHeartbeatSafely() {
        try {
            sendHeartbeatAll();
        } catch (Exception e) {
            log.warn("[SseEmitterService.runHeartbeatSafely] heartbeat iteration failed", e);
        }
    }

    private void sendHeartbeatAll() {
        sseStreamingMap.forEach((diningId, emitters) ->
            emitters.forEach((userId, emitter) -> {
                try {
                    emitter.send(
                        SseEmitter.event()
                            .name(HEARTBEAT.getValue())
                            .comment("heartbeat")
                            .reconnectTime(RECONNECTION_TIMEOUT)
                    );
                } catch (Exception e) {
                    log.debug(
                        "[SseEmitterService.heartbeat] failed. diningId={}, userId={}", diningId, userId
                    );
                    removeIfSame(diningId, userId, emitter, "heartbeat-error", e);
                    safeCompleteWithError(emitter, e);
                }
            })
        );
    }
}
