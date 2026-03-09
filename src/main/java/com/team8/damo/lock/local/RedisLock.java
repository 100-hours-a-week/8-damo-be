package com.team8.damo.lock.local;

import com.team8.damo.aop.DistridutedLock;
import com.team8.damo.lock.LockStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class RedisLock implements LockStrategy {

    private final RedissonClient redissonClient;

    @Override
    public Object execute(String key, DistridutedLock distridutedLock, ProceedingJoinPoint joinPoint) throws Throwable {
        RLock rLock = redissonClient.getLock(key);

        try {
            boolean available = rLock.tryLock(distridutedLock.waitTime(), distridutedLock.leaseTime(), distridutedLock.timeUnit());
            if (!available) {
                return false;
            }

            return joinPoint.proceed();
        } catch (InterruptedException e) {
            throw new InterruptedException();
        }  finally {
            try {
                rLock.unlock();
            } catch (IllegalMonitorStateException e) {
                log.info("Redisson Lock Already UnLock {} ",
                    kv("key", key)
                );
            }
        }
    }
}
