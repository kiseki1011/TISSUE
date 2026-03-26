package com.tissue.security.config;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.security")
public class SecurityProperties {

    private List<String> authProviders = List.of("EMAIL");

    private Jwt jwt = new Jwt();

    private Cors cors = new Cors();

    private OAuth2 oauth2 = new OAuth2();

    private Cookie cookie = new Cookie();

    @Data
    public static class Jwt {
        private String secret = "";
        private Duration accessTokenValidity = Duration.ofHours(1);
        private Duration refreshTokenValidity = Duration.ofDays(7);
        private Duration elevatedTokenValidity = Duration.ofMinutes(10);
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("*");
    }

    @Data
    public static class OAuth2 {
        private List<String> allowedRedirectOrigins = List.of("*");
    }

    @Data
    public static class Cookie {
        private boolean secure = false;
    }
}
