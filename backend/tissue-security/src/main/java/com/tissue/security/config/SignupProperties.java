package com.tissue.security.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.security.signup")
public class SignupProperties {

    private boolean enabled = true;
    private List<String> allowedDomains = List.of();

    public boolean isAllDomainsAllowed() {
        return allowedDomains != null && allowedDomains.contains("*");
    }

    public boolean isDomainRestricted() {
        return !isAllDomainsAllowed() && allowedDomains != null && !allowedDomains.isEmpty();
    }
}
