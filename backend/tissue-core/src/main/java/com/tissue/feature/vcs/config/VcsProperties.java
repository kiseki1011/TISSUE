package com.tissue.feature.vcs.config;

import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.support.WebhookUrlProvider;
import java.util.Locale;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class VcsProperties implements WebhookUrlProvider {

    @Value("${tissue.base-url}")
    private String baseUrl;

    @Override
    public String buildWebhookUrl(String projectKey, VcsProvider provider) {
        String providerName = provider.name().toLowerCase(Locale.ROOT);
        return String.format("%s/api/v1/projects/%s/integrations/%s/webhook", baseUrl, projectKey, providerName);
    }
}
