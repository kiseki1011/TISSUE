package com.tissue.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.vcs.adapter.in.web.dto.response.VcsIntegrationDetail;
import com.tissue.vcs.adapter.in.web.dto.response.VcsSecretResponse;
import com.tissue.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.vcs.domain.enums.VcsProvider;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
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
class WorkspaceVcsIntegrationServiceTest {

    @InjectMocks
    private WorkspaceVcsIntegrationService sut;

    @Mock
    private WorkspaceVcsIntegrationRepository repository;

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    private final String workspaceKey = "WS-KEY";
    private final String webhookUrlBase = "http://localhost:8080/api/v1/integrations/%s/github/webhook";

    @Test
    @DisplayName("시크릿을 재생성하면 기존 연동이 있을 경우 시크릿을 교체한다")
    void regenerateSecret_Existing() {
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
    @DisplayName("시크릿을 재생성할 때 기존 연동이 없으면 새로 생성한다")
    void regenerateSecret_New() {
        WorkspaceMemberContext context = mock(WorkspaceMemberContext.class);

        ReflectionTestUtils.setField(sut, "appBaseUrl", "http://localhost:8080");

        given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                .willReturn(Optional.empty());

        given(repository.save(any(WorkspaceVcsIntegration.class))).willAnswer(invocation -> invocation.getArgument(0));

        VcsSecretResponse response = sut.regenerateSecret(workspaceKey, VcsProvider.GITHUB, context);

        then(workspaceAuthorizationService).should().requireWorkspaceAdmin(context);
        then(repository).should().save(any(WorkspaceVcsIntegration.class));
        assertThat(response.webhookUrl()).isEqualTo(webhookUrlBase.formatted(workspaceKey));
    }

    @Test
    @DisplayName("연동 정보를 삭제한다 (Soft Delete)")
    void removeIntegration() {
        WorkspaceMemberContext context = mock(WorkspaceMemberContext.class);
        WorkspaceVcsIntegration integration = mock(WorkspaceVcsIntegration.class);

        given(repository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                .willReturn(Optional.of(integration));

        sut.removeIntegration(workspaceKey, VcsProvider.GITHUB, context);

        then(workspaceAuthorizationService).should().requireWorkspaceAdmin(context);
        then(integration).should().softDelete();
    }

    @Test
    @DisplayName("연동 정보를 조회한다")
    void getIntegration() {
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
