package com.tissue.feature.vcs.web.config;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.support.WebhookUrlProvider;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tissue.vcs")
public class VcsProperties implements WebhookUrlProvider {

    /**
     * The base URL of the application (Example: https://api.tissue.com).
     */
    private String baseUrl = "http://localhost:8080";

    private String webhookPathTemplate = "/api/v1/workspaces/%s/integrations/%s/webhook";

    @Override
    public String buildWebhookUrl(String workspaceKey, VcsProvider provider) {
        String providerName = provider.name().toLowerCase(Locale.ROOT);
        return baseUrl + String.format(webhookPathTemplate, workspaceKey, providerName);
    }
}
