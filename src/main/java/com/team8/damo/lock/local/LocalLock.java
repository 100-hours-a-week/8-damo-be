package com.team8.damo.lock.local;

import com.team8.damo.aop.CustomLock;
import com.team8.damo.lock.LockStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalLock implements LockStrategy {
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();

    @Override
    public Object execute(String key, CustomLock customLock, ProceedingJoinPoint joinPoint) throws Throwable {
        AtomicInteger counter = counterMap.computeIfAbsent(key, k -> new AtomicInteger(0));
        counter.incrementAndGet();

        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock(true));

        if (!lock.tryLock(customLock.waitTime(), customLock.timeUnit())) {
            counter.decrementAndGet();
            throw new RuntimeException();
        }

        try {
            log.info("[LocalLock.executor] catch lock key: {}", key);
            return joinPoint.proceed();
        } finally {
            lock.unlock();
            if (counter.decrementAndGet() == 0) {
                counterMap.remove(key);
                lockMap.remove(key);
            }
            log.info("[LocalLock.executor] catch unlock key: {}", key);
        }
    }
}
