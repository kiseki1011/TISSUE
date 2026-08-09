package com.tissue.feature.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsWebhookDeliverySummary;
import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.application.port.repository.VcsWebhookDeliveryRepository;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.VcsWebhookDelivery;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.enums.WebhookDeliveryStatus;
import com.tissue.feature.vcs.domain.exception.ProjectVcsIntegrationNotFoundException;
import com.tissue.feature.vcs.domain.support.WebhookUrlProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ProjectVcsServiceTest {

    @Mock
    private ProjectVcsIntegrationRepository integrationRepository;

    @Mock
    private ProjectAccessResolver projectAccessResolver;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @Mock
    private WebhookUrlProvider webhookUrlProvider;

    @Mock
    private VcsWebhookDeliveryRepository deliveryRepository;

    @InjectMocks
    private ProjectVcsService sut;

    private static final String PROJECT_KEY = "PROJ";
    private static final Long ACTOR_ID = 1L;

    @Nested
    @DisplayName("toggle sync")
    class ToggleSync {

        @Test
        @DisplayName("success: disabling sync leaves the integration in place")
        void disablingKeepsIntegration() {
            // given
            ProjectVcsIntegration integration = givenIntegration();

            // when
            VcsIntegrationDetail detail = sut.setSyncEnabled(PROJECT_KEY, VcsProvider.GITHUB, false, ACTOR_ID);

            // then
            assertThat(detail.isSyncEnabled()).isFalse();
            assertThat(integration.isInactive()).isTrue();
            then(integrationRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("success: re-enabling sync needs no new secret")
        void reEnablingKeepsSecret() {
            // given
            ProjectVcsIntegration integration = givenIntegration();
            integration.toggleSync(false);

            // when
            VcsIntegrationDetail detail = sut.setSyncEnabled(PROJECT_KEY, VcsProvider.GITHUB, true, ACTOR_ID);

            // then
            assertThat(detail.isSyncEnabled()).isTrue();
            assertThat(integration.getWebhookSecret()).isEqualTo("secret");
        }

        @Test
        @DisplayName("success: requires project manager")
        void requiresProjectManager() {
            // given
            ProjectMember actor = givenIntegrationWithActor();

            // when
            sut.setSyncEnabled(PROJECT_KEY, VcsProvider.GITHUB, false, ACTOR_ID);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
        }

        @Test
        @DisplayName("fail: no integration to toggle")
        void failsWhenNoIntegration() {
            // given
            given(projectAccessResolver.resolveByProjectKey(PROJECT_KEY, ACTOR_ID))
                    .willReturn(mock(ProjectMember.class));
            given(integrationRepository.findByProjectKeyAndProvider(PROJECT_KEY, VcsProvider.GITHUB))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.setSyncEnabled(PROJECT_KEY, VcsProvider.GITHUB, false, ACTOR_ID))
                    .isInstanceOf(ProjectVcsIntegrationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("list deliveries")
    class ListDeliveries {

        @Test
        @DisplayName("success: requires project manager")
        void requiresProjectManager() {
            // given
            ProjectMember actor = mock(ProjectMember.class);
            given(projectAccessResolver.resolveByProjectKey(PROJECT_KEY, ACTOR_ID))
                    .willReturn(actor);
            given(deliveryRepository.findByProjectKeyAndProvider(
                            eq(PROJECT_KEY), eq(VcsProvider.GITHUB), any(Pageable.class)))
                    .willReturn(Page.empty());

            // when
            sut.getDeliveries(PROJECT_KEY, VcsProvider.GITHUB, PageRequest.of(0, 20), ACTOR_ID);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
        }

        @Test
        @DisplayName("success: a caller's sort is replaced by a stable newest-first order")
        void forcesStableNewestFirstOrder() {
            // given
            given(projectAccessResolver.resolveByProjectKey(PROJECT_KEY, ACTOR_ID))
                    .willReturn(mock(ProjectMember.class));
            given(deliveryRepository.findByProjectKeyAndProvider(
                            eq(PROJECT_KEY), eq(VcsProvider.GITHUB), any(Pageable.class)))
                    .willReturn(Page.empty());
            Pageable callerSort = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "eventType"));

            // when
            sut.getDeliveries(PROJECT_KEY, VcsProvider.GITHUB, callerSort, ACTOR_ID);

            // then
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            then(deliveryRepository)
                    .should()
                    .findByProjectKeyAndProvider(eq(PROJECT_KEY), eq(VcsProvider.GITHUB), captor.capture());
            assertThat(captor.getValue().getSort())
                    .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        }

        @Test
        @DisplayName("success: the summary carries why a delivery was ignored")
        void summaryCarriesDiagnosticDetail() {
            // given
            given(projectAccessResolver.resolveByProjectKey(PROJECT_KEY, ACTOR_ID))
                    .willReturn(mock(ProjectMember.class));
            VcsWebhookDelivery delivery =
                    VcsWebhookDelivery.create(VcsProvider.GITHUB, "d-1", PROJECT_KEY, "push", "{}");
            delivery.markIgnored("No issue key found in: refs/heads/fix-login");
            given(deliveryRepository.findByProjectKeyAndProvider(
                            eq(PROJECT_KEY), eq(VcsProvider.GITHUB), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(delivery)));

            // when
            Page<VcsWebhookDeliverySummary> page =
                    sut.getDeliveries(PROJECT_KEY, VcsProvider.GITHUB, PageRequest.of(0, 20), ACTOR_ID);

            // then
            VcsWebhookDeliverySummary summary = page.getContent().getFirst();
            assertThat(summary.status()).isEqualTo(WebhookDeliveryStatus.IGNORED);
            assertThat(summary.resultDetail()).isEqualTo("No issue key found in: refs/heads/fix-login");
        }
    }

    private ProjectVcsIntegration givenIntegration() {
        givenIntegrationWithActor();
        return integrationRepository
                .findByProjectKeyAndProvider(PROJECT_KEY, VcsProvider.GITHUB)
                .orElseThrow();
    }

    private ProjectMember givenIntegrationWithActor() {
        ProjectMember actor = mock(ProjectMember.class);
        given(projectAccessResolver.resolveByProjectKey(PROJECT_KEY, ACTOR_ID)).willReturn(actor);
        given(integrationRepository.findByProjectKeyAndProvider(PROJECT_KEY, VcsProvider.GITHUB))
                .willReturn(Optional.of(ProjectVcsIntegration.create(VcsProvider.GITHUB, PROJECT_KEY, "secret")));
        return actor;
    }
}
