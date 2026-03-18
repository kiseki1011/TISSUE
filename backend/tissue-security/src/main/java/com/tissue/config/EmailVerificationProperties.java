package com.tissue.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tissue.email.verification")
public class EmailVerificationProperties {

    private String baseUrl = "http://localhost:8080";
    private Duration emailTtl = Duration.ofMinutes(30);
    private Duration verifiedTokenTtl = Duration.ofMinutes(10);
}
