package com.tissue.adapter.web.aop;

import com.tissue.adapter.web.annotation.RateLimit;
import com.tissue.application.port.repository.RateLimitStore;
import com.tissue.shared.exception.CommonErrorCode;
import com.tissue.shared.exception.base.RateLimitExceededException;
import java.lang.reflect.Method;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Applies rate limiting to methods annotated with {@link RateLimit}
 * using a fixed window counter.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate:";

    private final RateLimitStore rateLimitStore;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String keyValue = extractKey(joinPoint, rateLimit.key());
        String fullKey = KEY_PREFIX + rateLimit.prefix() + ":" + keyValue;
        Duration window = Duration.of(rateLimit.window(), rateLimit.timeUnit().toChronoUnit());

        int count = rateLimitStore.incrementAndGet(fullKey, window);
        if (count > rateLimit.maxRequests()) {
            throw new RateLimitExceededException(CommonErrorCode.RATE_LIMITED);
        }

        return joinPoint.proceed();
    }

    // TODO: GraalVM migration requires reflection hints for DTOs used with @RateLimit.
    //  use RuntimeHintsRegistrar to register the request DTOs
    private String extractKey(ProceedingJoinPoint joinPoint, String keyName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(keyName)) {
                return String.valueOf(args[i]);
            }
        }

        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                Method accessor = arg.getClass().getMethod(keyName);
                Object value = accessor.invoke(arg);
                if (value != null) {
                    return value.toString();
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        throw new IllegalStateException("Rate limit key '" + keyName + "' not found in method parameters of "
                + signature.getMethod().getName());
    }
}
