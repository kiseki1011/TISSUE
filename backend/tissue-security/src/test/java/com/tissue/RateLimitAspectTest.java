package com.tissue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tissue.security.adapter.web.annotation.RateLimit;
import com.tissue.security.adapter.web.aop.RateLimitAspect;
import com.tissue.security.application.port.repository.RateLimitStore;
import com.tissue.shared.exception.base.RateLimitExceededException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    RateLimitStore rateLimitStore;

    @Mock
    ProceedingJoinPoint joinPoint;

    @Mock
    MethodSignature signature;

    @Mock
    RateLimit rateLimit;

    @InjectMocks
    RateLimitAspect sut;

    @Nested
    @DisplayName("check rate-limit")
    class CheckRateLimit {

        @Test
        @DisplayName("success: proceeds when count is within limit")
        void successProceeds_When_WithinLimit() throws Throwable {
            String email = "test@tissue.com";
            Object expectedResult = "ok";

            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[] {"email"});
            given(joinPoint.getArgs()).willReturn(new Object[] {email});

            given(rateLimit.key()).willReturn("email");
            given(rateLimit.prefix()).willReturn("password-reset");
            given(rateLimit.maxRequests()).willReturn(5);
            given(rateLimit.window()).willReturn(1);
            given(rateLimit.timeUnit()).willReturn(TimeUnit.HOURS);

            given(rateLimitStore.incrementAndGet(eq("rate:password-reset:" + email), any(Duration.class)))
                    .willReturn(1);
            given(joinPoint.proceed()).willReturn(expectedResult);

            Object result = sut.checkRateLimit(joinPoint, rateLimit);

            assertThat(result).isEqualTo(expectedResult);
            then(joinPoint).should().proceed();
        }

        @Test
        @DisplayName("fail: throws exception if count exceeds limit")
        void failRateLimitCheck_When_ExceedLimit() throws Throwable {
            // given
            String email = "test@tissue.com";
            int maxRequests = 5;

            given(joinPoint.getSignature()).willReturn(signature);
            given(signature.getParameterNames()).willReturn(new String[] {"email"});
            given(joinPoint.getArgs()).willReturn(new Object[] {email});

            given(rateLimit.key()).willReturn("email");
            given(rateLimit.prefix()).willReturn("password-reset");
            given(rateLimit.maxRequests()).willReturn(maxRequests);
            given(rateLimit.window()).willReturn(1);
            given(rateLimit.timeUnit()).willReturn(TimeUnit.HOURS);

            given(rateLimitStore.incrementAndGet(eq("rate:password-reset:" + email), any(Duration.class)))
                    .willReturn(maxRequests + 1);

            // when & then
            assertThatThrownBy(() -> sut.checkRateLimit(joinPoint, rateLimit))
                    .isInstanceOf(RateLimitExceededException.class);

            then(joinPoint).should(never()).proceed();
        }
    }
}
