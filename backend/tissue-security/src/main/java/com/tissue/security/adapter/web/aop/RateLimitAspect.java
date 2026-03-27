package com.tissue.security.adapter.web.aop;

import com.tissue.security.adapter.web.annotation.RateLimit;
import com.tissue.security.application.port.repository.RateLimitStore;
import com.tissue.shared.exception.CommonErrorCode;
import com.tissue.shared.exception.base.RateLimitExceededException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/**
 * Applies rate limiting to methods annotated with {@link RateLimit}
 * using a fixed window counter.
 */
@Deprecated
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate:";
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer DISCOVERER = new DefaultParameterNameDiscoverer();

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

    private String extractKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        EvaluationContext context =
                new MethodBasedEvaluationContext(null, signature.getMethod(), joinPoint.getArgs(), DISCOVERER);

        Object value = PARSER.parseExpression(keyExpression).getValue(context);
        if (value == null) {
            throw new IllegalStateException("Rate limit key expression '" + keyExpression
                    + "' evaluated to null for method " + signature.getMethod().getName());
        }
        return value.toString();
    }
}
