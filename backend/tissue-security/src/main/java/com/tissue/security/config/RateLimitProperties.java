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
    private Webhook webhook = new Webhook();

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

    /**
     * Inbound VCS webhooks. The endpoint is unauthenticated, so a caller with no valid signature still
     * costs an integration lookup and an HMAC computation before being rejected; this caps how often that
     * can be paid for. The ceiling is far above real webhook traffic, which is a handful per minute.
     */
    @Data
    public static class Webhook {
        private int maxAttempts = 120;
        private Duration window = Duration.ofMinutes(1);
    }
}
