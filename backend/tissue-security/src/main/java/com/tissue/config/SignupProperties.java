package com.tissue.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.security.signup")
public class SignupProperties {

    private boolean allowSignup = true;
    private List<String> allowedDomains = List.of();

    public boolean isDomainRestricted() {
        return allowedDomains != null && !allowedDomains.isEmpty();
    }
}
