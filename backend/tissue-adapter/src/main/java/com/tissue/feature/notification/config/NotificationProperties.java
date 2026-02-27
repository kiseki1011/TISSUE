package com.tissue.feature.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tissue.notification")
public record NotificationProperties(EmailProperties email) {

    public record EmailProperties(int concurrency, long retryIntervalMs) {}
}
