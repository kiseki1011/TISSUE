package com.tissue.config;

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

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("*");
    }
}
