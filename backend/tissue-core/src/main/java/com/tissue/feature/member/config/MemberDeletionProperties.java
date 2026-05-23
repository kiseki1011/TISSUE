package com.tissue.feature.member.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.member.deletion")
public class MemberDeletionProperties {

    private Duration retention = Duration.ofDays(7);
    private String cleanupCron = "0 0 3 * * *";
}
