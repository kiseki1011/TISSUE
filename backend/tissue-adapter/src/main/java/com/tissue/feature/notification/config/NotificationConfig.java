package com.tissue.feature.notification.config;

import com.tissue.feature.notification.sender.EmailThrottledExecutor;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {

    private final NotificationProperties properties;

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        return new EmailThrottledExecutor(properties.email().concurrency());
    }
}
