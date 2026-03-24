package com.tissue.feature.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.repository.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.support.WebhookUrlProvider;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceVcsServiceTest {

    @Mock
    private WorkspaceVcsIntegrationRepository repository;

    @Mock
    private WorkspaceMemberFinder workspaceMemberFinder;

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    @Mock
    private WebhookUrlProvider webhookUrlProvider;

    @InjectMocks
    private WorkspaceVcsService sut;

    @Nested
    @DisplayName("regenerate secret")
    class RegenerateSecret {

        @Test
        @DisplayName("success: rotates secret if integration exists")
        void successRegenerate_RotateSecret_If_IntegrationExist() {
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;
            String webhookUrl = "http://localhost:8080/api/v1/workspaces/WORKSPACE/integrations/github/webhook";

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WorkspaceVcsIntegration integration = mock(WorkspaceVcsIntegration.class);

            given(webhookUrlProvider.buildWebhookUrl(eq(workspaceKey), any(VcsProvider.class)))
                    .willReturn(webhookUrl);
            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            given(integration.getWebhookSecret()).willReturn("new-secret");

            VcsSecretResponse response = sut.regenerateSecret(workspaceKey, VcsProvider.GITHUB, actorMemberId);

            then(workspaceAuthorizationService).should().requireWorkspaceAdmin(actor);
            then(integration).should().rotateSecret(any());
            assertThat(response.webhookUrl()).isEqualTo(webhookUrl.formatted(workspaceKey));
        }

        @Test
        @DisplayName("success: creates new integration if not exists")
        void successRegenerateSecret_If_New() {
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;
            String webhookUrl = "http://localhost:8080/api/v1/workspaces/WORKSPACE/integrations/github/webhook";

            WorkspaceMember actor = mock(WorkspaceMember.class);

            given(webhookUrlProvider.buildWebhookUrl(eq(workspaceKey), any(VcsProvider.class)))
                    .willReturn(webhookUrl);
            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.empty());

            given(repository.save(any(WorkspaceVcsIntegration.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            VcsSecretResponse response = sut.regenerateSecret(workspaceKey, VcsProvider.GITHUB, actorMemberId);

            then(workspaceAuthorizationService).should().requireWorkspaceAdmin(actor);
            then(repository).should().save(any(WorkspaceVcsIntegration.class));
            assertThat(response.webhookUrl()).isEqualTo(webhookUrl.formatted(workspaceKey));
        }
    }

    @Nested
    @DisplayName("remove integration")
    class RemoveIntegration {

        @Test
        @DisplayName("success: deletes integration")
        void successRemoveIntegration() {
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WorkspaceVcsIntegration integration = mock(WorkspaceVcsIntegration.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            sut.removeIntegration(workspaceKey, VcsProvider.GITHUB, actorMemberId);

            then(workspaceAuthorizationService).should().requireWorkspaceAdmin(actor);
            then(repository).should().delete(any(WorkspaceVcsIntegration.class));
        }
    }

    @Nested
    @DisplayName("get integration")
    class GetIntegration {

        @Test
        @DisplayName("success: returns integration detail")
        void successGetIntegration() {
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;
            String webhookUrl = "http://localhost:8080/api/v1/workspaces/WORKSPACE/integrations/github/webhook";

            WorkspaceMember actor = mock(WorkspaceMember.class);
            WorkspaceVcsIntegration integration = mock(WorkspaceVcsIntegration.class);

            given(webhookUrlProvider.buildWebhookUrl(eq(workspaceKey), any(VcsProvider.class)))
                    .willReturn(webhookUrl);
            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            given(integration.getId()).willReturn(1L);
            given(integration.isActive()).willReturn(true);
            given(integration.getWorkspaceKey()).willReturn(workspaceKey);

            VcsIntegrationDetail detail = sut.getIntegration(workspaceKey, VcsProvider.GITHUB, actorMemberId);

            assertThat(detail.webhookUrl()).isEqualTo(webhookUrl);
            assertThat(detail.isSyncEnabled()).isTrue();
        }
    }
}
