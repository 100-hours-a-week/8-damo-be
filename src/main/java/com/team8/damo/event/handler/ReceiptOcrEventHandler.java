package com.team8.damo.event.handler;

import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.payload.ReceiptOcrResponseEventPayload;
import com.team8.damo.redis.key.RedisKeyPrefix;
import com.team8.damo.service.DiningService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.team8.damo.entity.enumeration.OcrStatus.FAIL;
import static com.team8.damo.entity.enumeration.OcrStatus.SUCCESS;

@Component
@RequiredArgsConstructor
public class ReceiptOcrEventHandler implements EventHandler<ReceiptOcrResponseEventPayload> {

    private final DiningService diningService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void handle(Event<ReceiptOcrResponseEventPayload> event) {
        ReceiptOcrResponseEventPayload payload = event.getPayload();

        String key = RedisKeyPrefix.DINING_OCR_STATUS.key(payload.diningId());
        redisTemplate.opsForValue().set(key, payload.success() ? SUCCESS.name() : FAIL.name());

        if (payload.success()) {
            diningService.completeDining(payload.diningId());
        }
    }

    @Override
    public boolean supports(Event<ReceiptOcrResponseEventPayload> event) {
        return EventType.RECEIPT_OCR_RESPONSE == event.getEventType();
    }
}
