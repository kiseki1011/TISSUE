package com.tissue.security.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.security.rate-limit")
public class RateLimitProperties {

    private Login login = new Login();
    private EmailVerification emailVerification = new EmailVerification();
    private PasswordReset passwordReset = new PasswordReset();

    @Data
    public static class Login {
        private int maxAttempts = 10;
        private Duration window = Duration.ofMinutes(30);
    }

    @Data
    public static class EmailVerification {
        private int maxAttempts = 5;
        private Duration window = Duration.ofMinutes(30);
    }

    @Data
    public static class PasswordReset {
        private int maxAttempts = 5;
        private Duration window = Duration.ofMinutes(30);
    }
}
