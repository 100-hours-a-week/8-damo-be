package com.team8.damo.service;

import com.team8.damo.entity.User;
import com.team8.damo.event.payload.RecommendationStreamingEventPayload;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.response.RecommendationStreamingResponse;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private static final long TIMEOUT = 10 * 60 * 1000L;
    private static final long RECONNECTION_TIMEOUT = 1000L;
    private static final String EVENT_CONNECTED = "connected";

    private final Map<Long, Map<Long, SseEmitter>> sseStreamingMap = new ConcurrentHashMap<>();

    private final Snowflake snowflake;
    private final UserRepository userRepository;

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

        sendEvent(diningId, userId, newEmitter, EVENT_CONNECTED, "SSE connected");
        return newEmitter;
    }

    private void removeIfSame(Long diningId, Long userId, SseEmitter expected, String reason, Throwable error) {
        sseStreamingMap.computeIfPresent(diningId, (id, emitters) -> {
            emitters.compute(userId, (uid, current) -> current == expected ? null : current);
            return emitters.isEmpty() ? null : emitters;
        });
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
        Map<Long, SseEmitter> emitters = sseStreamingMap.get(diningId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        User user = userRepository.findById(data.userId()).orElse(null);

        RecommendationStreamingResponse streamingResponse = RecommendationStreamingResponse.builder()
            .eventId(snowflake.nextId())
            .userId(data.userId())
            .nickname(user == null ? "익명" : user.getNickname())
            .content(data.content())
            .createdAt(LocalDateTime.now())
            .build();

        emitters.forEach((userId, emitter) ->
            sendEvent(diningId, userId, emitter, eventName, streamingResponse)
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

    private void sendEvent(Long diningId, Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(eventName)
                    .reconnectTime(RECONNECTION_TIMEOUT)
                    .data(data)
            );
            log.info("[SseEmitterService.sendEvent] data={}", data);
        } catch (Exception e) {
            removeEmitter(diningId, userId, "send-error", e);
        }
    }

    private void removeEmitter(Long diningId, Long userId, String reason, Throwable error) {
        Map<Long, SseEmitter> emitters = sseStreamingMap.get(diningId);
        if (emitters == null) {
            return;
        }

        SseEmitter removedEmitter = emitters.remove(userId);
        if (emitters.isEmpty()) {
            sseStreamingMap.remove(diningId);
        }

        if (removedEmitter != null) {
            if (error == null) {
                log.info(
                    "[SseEmitterService.removeEmitter] removed. diningId={}, userId={}, reason={}",
                    diningId, userId, reason
                );
            } else {
                log.warn(
                    "[SseEmitterService.removeEmitter] removed. diningId={}, userId={}, reason={}",
                    diningId, userId, reason, error
                );
            }
        }
    }
}
