package com.tissue.global.config;

import com.tissue.global.concurrent.ThrottledExecutor;
import com.tissue.global.config.properties.NotificationProperties;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@EnableAsync
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NotificationProperties.class)
public class AsyncConfig implements AsyncConfigurer {

    private final NotificationProperties notificationProperties;

    @Bean(name = "notificationTaskExecutor")
    @Primary
    public Executor notificationTaskExecutor() {
        var properties = notificationProperties.executor();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.corePoolSize());
        executor.setMaxPoolSize(properties.maxPoolSize());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix(properties.threadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        return new ThrottledExecutor(notificationProperties.email().concurrency());
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("[ASYNC TASK FAILED] method {}: {}", method.getName(), ex.getMessage(), ex);
            log.error("method parameters: {}", Arrays.toString(params));
        };
    }
}
