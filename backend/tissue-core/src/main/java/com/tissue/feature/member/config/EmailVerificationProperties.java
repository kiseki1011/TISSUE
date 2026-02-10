package com.tissue.feature.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.email.verification")
public class EmailVerificationProperties {

    private String successUrl = "";
    private String failureUrl = "";
    private String verificationUrl = "";
    private java.time.Duration ttl = java.time.Duration.ofMinutes(30);
}
