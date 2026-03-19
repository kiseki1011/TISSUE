package com.tissue.security.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tissue.security.rate-limit")
public class RateLimitProperties {

    private Login login = new Login();

    @Data
    public static class Login {
        private int maxAttempts = 10;
        private Duration window = Duration.ofMinutes(30);
    }
}
