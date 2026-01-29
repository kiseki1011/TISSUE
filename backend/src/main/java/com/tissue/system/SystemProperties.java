package com.tissue.system;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tissue.system")
public class SystemProperties {

    private Mode mode = Mode.PUBLIC;

    private String serverName = "Tissue Server";

    public enum Mode {
        PUBLIC,
        PRIVATE
    }
}
