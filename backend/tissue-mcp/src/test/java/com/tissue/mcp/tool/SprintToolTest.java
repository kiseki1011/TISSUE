package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.port.usecase.SprintCommandUseCase;
import com.tissue.feature.sprint.application.port.usecase.SprintQueryUseCase;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SprintToolTest {

    private final SprintQueryUseCase sprintQueryUseCase = mock(SprintQueryUseCase.class);
    private final SprintCommandUseCase sprintCommandUseCase = mock(SprintCommandUseCase.class);

    private final SprintTool tool = new SprintTool(sprintQueryUseCase, sprintCommandUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: the sprint methods are exposed as MCP tools")
    void registersSprintMethodsAsMcpTools() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("list_sprints", "get_sprint", "add_issues_to_sprint", "remove_issues_from_sprint");
    }

    @Test
    @DisplayName("success: planning a sprint is not something an agent can do through MCP")
    void exposesNoSprintLifecycleTool() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .doesNotContain("create_sprint", "start_sprint", "complete_sprint", "cancel_sprint", "delete_sprint");
    }

    @Test
    @DisplayName("success: sprints are listed newest first so paging cannot repeat or drop one")
    void listSprintsPagesInAStableOrder() {
        authenticate("SCOPE_READ");
        given(sprintQueryUseCase.getProjectSprints(any(), any(), any(), anyLong()))
                .willReturn(Page.empty());

        tool.listSprints("PROJ", null, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(sprintQueryUseCase).should().getProjectSprints(any(), any(), pageable.capture(), anyLong());

        Sort.Order order = pageable.getValue().getSort().getOrderFor("sprintNumber");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("success: a status filter is read case-insensitively for the named project")
    void listSprintsParsesStatuses() {
        authenticate("SCOPE_READ");
        given(sprintQueryUseCase.getProjectSprints(any(), any(), any(), anyLong()))
                .willReturn(Page.empty());

        tool.listSprints("PROJ", List.of("active", "PLANNING"), null);

        ArgumentCaptor<ProjectIdentifier> pid = ArgumentCaptor.forClass(ProjectIdentifier.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<SprintStatus>> statuses = ArgumentCaptor.forClass(Set.class);
        then(sprintQueryUseCase).should().getProjectSprints(pid.capture(), statuses.capture(), any(), anyLong());

        assertThat(pid.getValue().projectKey()).isEqualTo("PROJ");
        assertThat(statuses.getValue()).containsExactlyInAnyOrder(SprintStatus.ACTIVE, SprintStatus.PLANNING);
    }

    @Test
    @DisplayName("fail: an unknown sprint status names the valid ones instead of silently widening the list")
    void unknownSprintStatusIsRejected() {
        authenticate("SCOPE_READ");

        assertThatThrownBy(() -> tool.listSprints("PROJ", List.of("DONE"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DONE")
                .hasMessageContaining("PLANNING, ACTIVE, COMPLETED, CANCELLED");

        then(sprintQueryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("success: the issue keys are left out unless they were asked for")
    void getSprintSkipsIssueKeysByDefault() {
        authenticate("SCOPE_READ");
        given(sprintQueryUseCase.getSprintDetail(1L, 7L))
                .willReturn(SprintDetail.builder().build());

        SprintTool.SprintView view = tool.getSprint(1L, null);

        assertThat(view.issueKeys()).isNull();
        then(sprintQueryUseCase).should(never()).getSprintIssueKeys(anyLong(), anyLong());
    }

    @Test
    @DisplayName("success: the issue keys are loaded when asked for")
    void getSprintLoadsIssueKeysOnRequest() {
        authenticate("SCOPE_READ");
        given(sprintQueryUseCase.getSprintDetail(1L, 7L))
                .willReturn(SprintDetail.builder().build());
        given(sprintQueryUseCase.getSprintIssueKeys(1L, 7L)).willReturn(new SprintIssueKeys(List.of("PROJ-1")));

        SprintTool.SprintView view = tool.getSprint(1L, true);

        assertThat(view.issueKeys()).containsExactly("PROJ-1");
    }

    @Test
    @DisplayName("success: scheduling issues into a sprint delegates for the calling agent")
    void addIssuesDelegates() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.addIssuesToSprint(1L, List.of("PROJ-1", "PROJ-2"));

        then(sprintCommandUseCase).should().addIssues(1L, List.of("PROJ-1", "PROJ-2"), 7L);
    }

    @Test
    @DisplayName("success: unscheduling issues delegates for the calling agent")
    void removeIssuesDelegates() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.removeIssuesFromSprint(1L, List.of("PROJ-1"));

        then(sprintCommandUseCase).should().removeIssues(1L, List.of("PROJ-1"), 7L);
    }

    @Test
    @DisplayName("fail: changing what a sprint holds needs a READ_WRITE token")
    void schedulingRequiresWriteScope() {
        authenticate("SCOPE_READ");

        assertThatThrownBy(() -> tool.addIssuesToSprint(1L, List.of("PROJ-1")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tool.removeIssuesFromSprint(1L, List.of("PROJ-1")))
                .isInstanceOf(AccessDeniedException.class);

        then(sprintCommandUseCase).shouldHaveNoInteractions();
        then(sprintCommandUseCase).should(never()).addIssues(eq(1L), anyList(), anyLong());
    }

    private void authenticate(String... scopes) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(scopes).map(SimpleGrantedAuthority::new).toList();
        MemberDetails principal = new MemberDetails(7L, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
