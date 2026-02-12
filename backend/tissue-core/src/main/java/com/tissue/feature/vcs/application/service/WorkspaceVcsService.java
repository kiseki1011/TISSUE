package com.tissue.feature.vcs.application.service;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.repository.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.WorkspaceVcsCommandUseCase;
import com.tissue.feature.vcs.application.port.usecase.WorkspaceVcsQueryUseCase;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceVcsService implements WorkspaceVcsCommandUseCase, WorkspaceVcsQueryUseCase {

    private final WorkspaceVcsIntegrationRepository repository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    // TODO: Use @Value
    private static final String WEBHOOK_PATH_TEMPLATE = "/api/v1/workspaces/%s/integrations/%s/webhook";

    @Override
    @Transactional
    public VcsSecretResponse regenerateSecret(String workspaceKey, VcsProvider provider, Long memberId) {

        WorkspaceMember actor = workspaceMemberFinder.getBy(workspaceKey, memberId);
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
    public void removeIntegration(String workspaceKey, VcsProvider provider, Long memberId) {
        WorkspaceMember actor = workspaceMemberFinder.getBy(workspaceKey, memberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        WorkspaceVcsIntegration integration = repository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(workspaceKey));

        integration.softDelete();
    }

    @Override
    @Transactional(readOnly = true)
    public VcsIntegrationDetail getIntegration(String workspaceKey, VcsProvider provider, Long memberId) {

        WorkspaceMember actor = workspaceMemberFinder.getBy(workspaceKey, memberId);
        workspaceAuthorizationService.requireWorkspaceMember(actor);

        WorkspaceVcsIntegration integration = repository
                .findByWorkspaceKeyAndProvider(workspaceKey, provider)
                .orElseThrow(() -> new WorkspaceVcsIntegrationNotFoundException(workspaceKey));

        return VcsIntegrationDetail.from(integration, buildWebhookUrl(workspaceKey, provider));
    }

    // TODO: Consider a better way
    private String generateRandomSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildWebhookUrl(String workspaceKey, VcsProvider provider) {
        return appBaseUrl
                + WEBHOOK_PATH_TEMPLATE.formatted(workspaceKey, provider.name().toLowerCase(Locale.ROOT));
    }
}
