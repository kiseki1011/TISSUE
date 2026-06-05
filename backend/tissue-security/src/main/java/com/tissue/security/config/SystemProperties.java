package com.tissue.security.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@SuppressWarnings("NullAway.Init")
@ConfigurationProperties(prefix = "tissue.system")
public class SystemProperties {

    @NotBlank
    private String version;

    private String serverName = "Tissue Server";
}
