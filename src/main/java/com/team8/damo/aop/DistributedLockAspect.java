package com.team8.damo.aop;

import com.team8.damo.lock.LockStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static net.logstash.logback.argument.StructuredArguments.kv;


@Slf4j
@Order(1)
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final LockStrategy lock;

    @Around("@annotation(com.team8.damo.aop.DistridutedLock)")
    public Object around(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistridutedLock distridutedLock = method.getAnnotation(DistridutedLock.class);

        String key = SpelKeyGenerator.getKey(distridutedLock.key(), joinPoint);

        try {
            return lock.execute(key, distridutedLock, joinPoint);
        } catch (Exception e) {
            log.error("Lock Already UnLock {} {}",
                kv("serviceName", method.getName()),
                kv("key", key),
                e
            );
            throw e;
        }
    }
}
