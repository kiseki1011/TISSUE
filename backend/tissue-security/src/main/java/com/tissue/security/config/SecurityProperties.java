package com.tissue.security.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.security")
public class SecurityProperties {

    private List<String> authProviders = List.of("EMAIL");

    private Cors cors = new Cors();

    private Cookie cookie = new Cookie();

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("*");
    }

    @Data
    public static class Cookie {
        private boolean secure = false;
    }
}
