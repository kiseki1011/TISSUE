package com.tissue.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.system")
public class SystemProperties {
    private String serverName = "Tissue Server";
}
