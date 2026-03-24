package com.tissue.security.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.email.verification")
public class EmailVerificationProperties {

    private String baseUrl = "http://localhost:8080";
    private String signupVerifyPath = "/api/v1/members/signup/verify";
    private String passwordResetVerifyPath = "/api/v1/members/password/verify";
    private Duration emailTtl = Duration.ofMinutes(30);
    private Duration verifiedTokenTtl = Duration.ofMinutes(10);
}
