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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceVcsService implements WorkspaceVcsCommandUseCase, WorkspaceVcsQueryUseCase {

    private final WorkspaceVcsIntegrationRepository integrationRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WebhookUrlProvider webhookUrlProvider;

    @Override
    @Transactional
    public VcsSecretResponse regenerateSecret(String workspaceKey, VcsProvider provider, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        Optional<WorkspaceVcsIntegration> existingIntegration =
                integrationRepository.findByWorkspaceKeyAndProvider(workspaceKey, provider);

        if (existingIntegration.isEmpty()) {
            WorkspaceVcsIntegration integration = createVcsIntegration(workspaceKey, provider);
            return new VcsSecretResponse(buildWebhookUrl(workspaceKey, provider), integration.getWebhookSecret());
        }

        WorkspaceVcsIntegration integration = existingIntegration.get();
        integration.rotateSecret(generateRandomSecret());

        return new VcsSecretResponse(buildWebhookUrl(workspaceKey, provider), integration.getWebhookSecret());
    }

    @Override
    @Transactional
    public void removeIntegration(String workspaceKey, VcsProvider provider, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(workspaceKey, provider.toString()));

        integrationRepository.delete(integration);
    }

    @Override
    @Transactional(readOnly = true)
    public VcsIntegrationDetail getIntegration(String workspaceKey, VcsProvider provider, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        WorkspaceVcsIntegration integration = integrationRepository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(workspaceKey, provider.toString()));

        return VcsIntegrationDetail.from(integration, buildWebhookUrl(workspaceKey, provider));
    }

    private WorkspaceVcsIntegration createVcsIntegration(String workspaceKey, VcsProvider provider) {
        return integrationRepository.save(
                WorkspaceVcsIntegration.create(provider, workspaceKey, generateRandomSecret()));
    }

    private String generateRandomSecret() {
        return KeyGenerators.string().generateKey();
    }

    private String buildWebhookUrl(String workspaceKey, VcsProvider provider) {
        return webhookUrlProvider.buildWebhookUrl(workspaceKey, provider);
    }
}
