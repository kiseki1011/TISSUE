package com.tissue.feature.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// TODO: 그냥 클래스로 사용하는걸 고려
//  - 위치가 여기에 있는게 맞을까?
@ConfigurationProperties(prefix = "tissue.notification")
public record NotificationProperties(EmailProperties email, ExecutorProperties executor) {

    public record EmailProperties(int concurrency, long retryIntervalMs) {}

    public record ExecutorProperties(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {}
}
