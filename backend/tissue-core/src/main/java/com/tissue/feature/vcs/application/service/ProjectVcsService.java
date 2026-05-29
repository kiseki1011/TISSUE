package com.tissue.feature.vcs.application.service;

import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.usecase.ProjectVcsCommandUseCase;
import com.tissue.feature.vcs.application.port.usecase.ProjectVcsQueryUseCase;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.ProjectVcsIntegrationNotFoundException;
import com.tissue.feature.vcs.domain.support.WebhookUrlProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectVcsService implements ProjectVcsCommandUseCase, ProjectVcsQueryUseCase {

    private final ProjectVcsIntegrationRepository integrationRepository;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final WebhookUrlProvider webhookUrlProvider;

    @Override
    @Transactional
    public VcsSecretResponse regenerateSecret(String projectKey, VcsProvider provider, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Optional<ProjectVcsIntegration> existingIntegration =
                integrationRepository.findByProjectKeyAndProvider(projectKey, provider);

        if (existingIntegration.isEmpty()) {
            ProjectVcsIntegration integration = createVcsIntegration(projectKey, provider);
            return new VcsSecretResponse(buildWebhookUrl(projectKey, provider), integration.getWebhookSecret());
        }

        ProjectVcsIntegration integration = existingIntegration.get();
        integration.rotateSecret(generateRandomSecret());

        return new VcsSecretResponse(buildWebhookUrl(projectKey, provider), integration.getWebhookSecret());
    }

    @Override
    @Transactional
    public void removeIntegration(String projectKey, VcsProvider provider, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        ProjectVcsIntegration integration = integrationRepository
                .findByProjectKeyAndProvider(projectKey, provider)
                .orElseThrow(() -> new ProjectVcsIntegrationNotFoundException(projectKey, provider.toString()));

        integrationRepository.delete(integration);
    }

    @Override
    @Transactional(readOnly = true)
    public VcsIntegrationDetail getIntegration(String projectKey, VcsProvider provider, Long actorMemberId) {
        projectMemberFinder.getByProjectKey(projectKey, actorMemberId);

        ProjectVcsIntegration integration = integrationRepository
                .findByProjectKeyAndProvider(projectKey, provider)
                .orElseThrow(() -> new ProjectVcsIntegrationNotFoundException(projectKey, provider.toString()));

        return VcsIntegrationDetail.from(integration, buildWebhookUrl(projectKey, provider));
    }

    private ProjectVcsIntegration createVcsIntegration(String projectKey, VcsProvider provider) {
        return integrationRepository.save(ProjectVcsIntegration.create(provider, projectKey, generateRandomSecret()));
    }

    private String generateRandomSecret() {
        return KeyGenerators.string().generateKey();
    }

    private String buildWebhookUrl(String projectKey, VcsProvider provider) {
        return webhookUrlProvider.buildWebhookUrl(projectKey, provider);
    }
}
