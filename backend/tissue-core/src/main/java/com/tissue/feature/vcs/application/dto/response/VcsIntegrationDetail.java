package com.tissue.feature.vcs.application.dto.response;

import com.tissue.feature.vcs.domain.ProjectVcsIntegration;

public record VcsIntegrationDetail(Long vcsIntegrationId, String projectKey, boolean isSyncEnabled, String webhookUrl) {

    public static VcsIntegrationDetail from(ProjectVcsIntegration integration, String webhookUrl) {
        return new VcsIntegrationDetail(
                integration.getId(), integration.getProjectKey(), integration.isActive(), webhookUrl);
    }
}
