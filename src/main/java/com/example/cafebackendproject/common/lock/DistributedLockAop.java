package com.example.cafebackendproject.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Order(0) // (락 획득 → 트랜잭션 시작 → 커밋 → 락 해제)
@Component
@RequiredArgsConstructor
public class DistributedLockAop {

    private static final String LOCK_PREFIX = "lock:";
    private final RedissonClient redissonClient;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(com.example.cafebackendproject.common.lock.DistributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        String lockKey = LOCK_PREFIX + resolveKey(annotation.key(), signature.getParameterNames(), joinPoint.getArgs());
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), annotation.timeUnit());
        if (!acquired) {
            log.warn("분산 락 획득 실패: {}", lockKey);
            throw new IllegalStateException("현재 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String resolveKey(String expression, String[] paramNames, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        return parser.parseExpression(expression).getValue(context, String.class);
    }
}