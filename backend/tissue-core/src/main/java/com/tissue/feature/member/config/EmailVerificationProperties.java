package com.tissue.feature.member.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.email.verification")
public class EmailVerificationProperties {

    private String baseUrl = "http://localhost:8080";
    private Duration verificationEmailTtl = Duration.ofMinutes(30);
    private Duration signupTokenTtl = Duration.ofMinutes(10);
    private Duration passwordResetCodeTtl = Duration.ofMinutes(10);
    private Duration passwordResetTokenTtl = Duration.ofMinutes(5);
}
