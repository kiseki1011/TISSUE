package com.tissue.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.deployment")
public class DeploymentProperties {
    private boolean multiTenant = false;
}
