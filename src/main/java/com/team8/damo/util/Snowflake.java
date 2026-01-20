package com.team8.damo.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.random.RandomGenerator;

/**
 * Snowflake 알고리즘 기반 분산 ID 생성기
 *
 * 구조: 64bit = 1bit(사용안함) + 41bit(timestamp) + 10bit(machine) + 12bit(sequence)
 * - 41bit timestamp: 약 69년간 사용 가능
 * - 10bit machine: 최대 1024개 머신 지원
 * - 12bit sequence: 밀리초당 4096개 ID 생성 가능
 */
@Component
public class Snowflake {
    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long maxNodeId = (1L << NODE_ID_BITS) - 1;
    private static final long maxSequence = (1L << SEQUENCE_BITS) - 1;

    private final long nodeId = RandomGenerator.getDefault().nextLong(maxNodeId + 1);
    private final long startTimeMillis = 1704067200000L;

    private long lastTimeMillis = startTimeMillis;
    private long sequence = 0L;

    private final Lock lock = new ReentrantLock();

    public long nextId() {
        lock.lock();
        try {
            long currentTimeMillis = System.currentTimeMillis();

            if (currentTimeMillis < lastTimeMillis) {
                throw new IllegalStateException("Invalid Time");
            }

            if (currentTimeMillis == lastTimeMillis) {
                sequence = (sequence + 1) & maxSequence;
                if (sequence == 0) {
                    currentTimeMillis = waitNextMillis(currentTimeMillis);
                }
            } else {
                sequence = 0;
            }

            lastTimeMillis = currentTimeMillis;

            return ((currentTimeMillis - startTimeMillis) << (NODE_ID_BITS + SEQUENCE_BITS))
                | (nodeId << SEQUENCE_BITS)
                | sequence;
        } finally {
            lock.unlock();
        }
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimeMillis) {
            currentTimestamp = System.currentTimeMillis();
        }
        return currentTimestamp;
    }
}
