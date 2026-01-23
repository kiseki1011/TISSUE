package com.tissue.vcs.adapter.in.web.dto.response;

import com.tissue.vcs.domain.WorkspaceVcsIntegration;

public record VcsIntegrationDetail(Long vcsIntegrationId, String workspaceKey, boolean isSyncEnabled, String webhookUrl) {

    public static VcsIntegrationDetail from(WorkspaceVcsIntegration integration, String webhookUrl) {
        return new VcsIntegrationDetail(
                integration.getId(), integration.getWorkspaceKey(), integration.isActive(), webhookUrl);
    }
}
