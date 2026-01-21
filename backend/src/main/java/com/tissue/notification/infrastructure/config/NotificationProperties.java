package com.tissue.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tissue.notification")
public record NotificationProperties(EmailProperties email, ExecutorProperties executor) {

    public record EmailProperties(int concurrency, long retryIntervalMs) {}

    public record ExecutorProperties(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {}
}
