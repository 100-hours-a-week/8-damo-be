package com.team8.damo.lock;

import com.team8.damo.aop.CustomLock;
import org.aspectj.lang.ProceedingJoinPoint;

public interface LockStrategy {
    Object execute(String key, CustomLock customLock, ProceedingJoinPoint joinPoint) throws Throwable;
}
