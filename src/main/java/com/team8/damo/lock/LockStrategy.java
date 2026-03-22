package com.team8.damo.lock;

import com.team8.damo.aop.DistridutedLock;
import org.aspectj.lang.ProceedingJoinPoint;

public interface LockStrategy {
    Object execute(String key, DistridutedLock distridutedLock, ProceedingJoinPoint joinPoint) throws Throwable;
}
