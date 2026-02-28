package com.tissue.feature.vcs.application.service;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.repository.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.WorkspaceVcsCommandUseCase;
import com.tissue.feature.vcs.application.port.usecase.WorkspaceVcsQueryUseCase;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.feature.vcs.domain.support.WebhookUrlProvider;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceVcsService implements WorkspaceVcsCommandUseCase, WorkspaceVcsQueryUseCase {

    private final WorkspaceVcsIntegrationRepository repository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WebhookUrlProvider webhookUrlProvider;

    @Override
    @Transactional
    public VcsSecretResponse regenerateSecret(String workspaceKey, VcsProvider provider, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        WorkspaceVcsIntegration integration = repository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElseGet(() -> WorkspaceVcsIntegration.create(provider, workspaceKey, generateRandomSecret()));

        if (integration.getId() != null) {
            integration.rotateSecret(generateRandomSecret());
        } else {
            integration = repository.save(integration);
        }

        if (integration.isSoftDeleted()) {
            integration.restoreSoftDeleted();
        }

        return new VcsSecretResponse(buildWebhookUrl(workspaceKey, provider), integration.getWebhookSecret());
    }

    @Override
    @Transactional
    public void removeIntegration(String workspaceKey, VcsProvider provider, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        WorkspaceVcsIntegration integration = repository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(workspaceKey, provider.toString()));

        integration.softDelete();
    }

    @Override
    @Transactional(readOnly = true)
    public VcsIntegrationDetail getIntegration(String workspaceKey, VcsProvider provider, Long actorMemberId) {
        workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);

        WorkspaceVcsIntegration integration = repository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(workspaceKey, provider.toString()));

        return VcsIntegrationDetail.from(integration, buildWebhookUrl(workspaceKey, provider));
    }

    private String generateRandomSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildWebhookUrl(String workspaceKey, VcsProvider provider) {
        return webhookUrlProvider.buildWebhookUrl(workspaceKey, provider);
    }
}
