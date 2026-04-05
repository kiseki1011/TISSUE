package com.tissue.security.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.email.verification")
public class EmailVerificationProperties {

    @Value("${tissue.base-url}")
    private String baseUrl;

    private String signupVerifyPath = "/api/v1/members/signup/verify";
    private String passwordResetVerifyPath = "/api/v1/members/password/verify";
    private Duration emailTtl = Duration.ofMinutes(30);
    private Duration verifiedTokenTtl = Duration.ofMinutes(10);
}
