package com.tissue.global.async;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Handles uncaught exceptions from {@code @Async} methods.
     * Provides centralized logging as the caller cannot intercept exceptions from void-returning methods.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error(
                    "Async task execution failed. method={}#{}, params={}, message={}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    Arrays.toString(params),
                    ex.getMessage(),
                    ex);
        };
    }
}
