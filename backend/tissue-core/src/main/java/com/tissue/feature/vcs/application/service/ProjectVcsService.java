package com.tissue.feature.vcs.application.service;

import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.dto.response.VcsWebhookDeliverySummary;
import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.repository.VcsWebhookDeliveryRepository;
import com.tissue.feature.vcs.application.port.usecase.ProjectVcsCommandUseCase;
import com.tissue.feature.vcs.application.port.usecase.ProjectVcsQueryUseCase;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.ProjectVcsIntegrationNotFoundException;
import com.tissue.feature.vcs.domain.support.WebhookUrlProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectVcsService implements ProjectVcsCommandUseCase, ProjectVcsQueryUseCase {

    private final ProjectVcsIntegrationRepository integrationRepository;
    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final WebhookUrlProvider webhookUrlProvider;
    private final VcsWebhookDeliveryRepository deliveryRepository;

    @Override
    @Transactional
    public VcsSecretResponse regenerateSecret(String projectKey, VcsProvider provider, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId);
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

    /**
     * Pauses or resumes acting on this project's webhooks without tearing the integration down. Removing it
     * instead would invalidate the secret, forcing the operator to reissue one and re-register it with the
     * provider just to turn automation back on.
     */
    @Override
    @Transactional
    public VcsIntegrationDetail setSyncEnabled(
            String projectKey, VcsProvider provider, boolean syncEnabled, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        ProjectVcsIntegration integration = integrationRepository
                .findByProjectKeyAndProvider(projectKey, provider)
                .orElseThrow(() -> new ProjectVcsIntegrationNotFoundException(projectKey, provider.toString()));

        integration.toggleSync(syncEnabled);

        return VcsIntegrationDetail.from(integration, buildWebhookUrl(projectKey, provider));
    }

    @Override
    @Transactional
    public void removeIntegration(String projectKey, VcsProvider provider, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId);
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

    /**
     * Lists what the provider has sent and how each delivery was handled, newest first. Restricted to a
     * manager because the rows carry operational detail, including failure reasons, rather than project
     * content. The sort is fixed rather than taken from the caller: a delivery log is only useful newest
     * first, and letting a client reorder it would page through an unstable ordering.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VcsWebhookDeliverySummary> getDeliveries(
            String projectKey, VcsProvider provider, Pageable pageable, Long actorMemberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        // id breaks ties: deliveries arriving in the same instant would otherwise page in an unstable
        // order, which drops or repeats rows across pages
        PageRequest newestFirst = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        return deliveryRepository
                .findByProjectKeyAndProvider(projectKey, provider, newestFirst)
                .map(VcsWebhookDeliverySummary::from);
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
