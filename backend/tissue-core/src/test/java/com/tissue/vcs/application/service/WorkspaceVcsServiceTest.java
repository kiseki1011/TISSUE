package com.tissue.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.application.service.WorkspaceVcsService;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceVcsServiceTest {

    @InjectMocks
    private WorkspaceVcsService sut;

    @Mock
    private WorkspaceVcsIntegrationRepository repository;

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    private final String workspaceKey = "WS-KEY";
    private final String webhookUrlBase = "http://localhost:8080/api/v1/workspaces/%s/integrations/github/webhook";

    @Nested
    @DisplayName("regenerate secret")
    class RegenerateSecret {

        @Test
        @DisplayName("success: rotates secret if integration exists")
        void success_Existing() {
            WorkspaceMemberContext context = mock(WorkspaceMemberContext.class);
            WorkspaceVcsIntegration integration = mock(WorkspaceVcsIntegration.class);

            ReflectionTestUtils.setField(sut, "appBaseUrl", "http://localhost:8080");

            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));
            given(integration.getId()).willReturn(1L);
            given(integration.getWebhookSecret()).willReturn("new-secret");

            VcsSecretResponse response = sut.regenerateSecret(workspaceKey, VcsProvider.GITHUB, context);

            then(workspaceAuthorizationService).should().requireWorkspaceAdmin(context);
            then(integration).should().rotateSecret(any());
            assertThat(response.webhookUrl()).isEqualTo(webhookUrlBase.formatted(workspaceKey));
        }

        @Test
        @DisplayName("success: creates new integration if not exists")
        void success_New() {
            WorkspaceMemberContext context = mock(WorkspaceMemberContext.class);

            ReflectionTestUtils.setField(sut, "appBaseUrl", "http://localhost:8080");

            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.empty());

            // mock save to return the argument (which is the new integration)
            given(repository.save(any(WorkspaceVcsIntegration.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            VcsSecretResponse response = sut.regenerateSecret(workspaceKey, VcsProvider.GITHUB, context);

            then(workspaceAuthorizationService).should().requireWorkspaceAdmin(context);
            then(repository).should().save(any(WorkspaceVcsIntegration.class));
            assertThat(response.webhookUrl()).isEqualTo(webhookUrlBase.formatted(workspaceKey));
        }
    }

    @Nested
    @DisplayName("remove integration")
    class RemoveIntegration {

        @Test
        @DisplayName("success: soft deletes integration")
        void success() {
            WorkspaceMemberContext context = mock(WorkspaceMemberContext.class);
            WorkspaceVcsIntegration integration = mock(WorkspaceVcsIntegration.class);

            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            sut.removeIntegration(workspaceKey, VcsProvider.GITHUB, context);

            then(workspaceAuthorizationService).should().requireWorkspaceAdmin(context);
            then(integration).should().softDelete();
        }
    }

    @Nested
    @DisplayName("get integration")
    class GetIntegration {

        @Test
        @DisplayName("success: returns integration detail")
        void success() {
            WorkspaceMemberContext context = mock(WorkspaceMemberContext.class);
            WorkspaceVcsIntegration integration = mock(WorkspaceVcsIntegration.class);

            ReflectionTestUtils.setField(sut, "appBaseUrl", "http://localhost:8080");

            given(context.isWorkspaceMember()).willReturn(true);
            given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));
            given(integration.getId()).willReturn(1L);
            given(integration.isActive()).willReturn(true);
            given(integration.getWorkspaceKey()).willReturn(workspaceKey);

            VcsIntegrationDetail detail = sut.getIntegration(workspaceKey, VcsProvider.GITHUB, context);

            then(workspaceAuthorizationService).should().requireWorkspaceMember(context);
            assertThat(detail.webhookUrl()).isEqualTo(webhookUrlBase.formatted(workspaceKey));
            assertThat(detail.isSyncEnabled()).isTrue();
        }
    }
}
