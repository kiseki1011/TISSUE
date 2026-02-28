package com.tissue.feature.vcs.domain.support;

import com.tissue.feature.vcs.domain.enums.VcsProvider;

public interface WebhookUrlProvider {

    String buildWebhookUrl(String workspaceKey, VcsProvider provider);
}
