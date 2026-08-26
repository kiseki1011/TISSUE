package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueListQueryUseCase;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class IssueListToolTest {

    private final IssueListQueryUseCase issueListQueryUseCase = mock(IssueListQueryUseCase.class);
    private final IssueFullTextSearchUseCase issueFtsUseCase = mock(IssueFullTextSearchUseCase.class);

    private final IssueListTool tool = new IssueListTool(issueListQueryUseCase, issueFtsUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: the discover work methods are exposed as MCP tools")
    void registersDiscoverToolsAsMcpTools() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("get_my_work", "get_backlog", "get_current_sprint_issues", "search_issues");
    }

    @Test
    @DisplayName("success: search without a project key searches every project the agent belongs to")
    void searchWithoutProjectKeySearchesEverywhere() {
        authenticate();
        given(issueFtsUseCase.ftsAllRanked(any(), anyInt(), anyInt(), anyLong()))
                .willReturn(Page.empty());

        tool.searchIssues("login timeout", null, null, null);

        then(issueFtsUseCase).should(never()).ftsByProjectRanked(any(), any(), anyInt(), anyInt(), anyLong());
        assertThat(conditionOfGlobalSearch().keyword()).isEqualTo("login timeout");
    }

    @Test
    @DisplayName("success: search with a project key is scoped to that project")
    void searchWithProjectKeyIsScoped() {
        authenticate();
        given(issueFtsUseCase.ftsByProjectRanked(any(), any(), anyInt(), anyInt(), anyLong()))
                .willReturn(Page.empty());

        tool.searchIssues("login timeout", "PROJ", null, 2);

        ArgumentCaptor<ProjectIdentifier> pid = ArgumentCaptor.forClass(ProjectIdentifier.class);
        ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
        then(issueFtsUseCase).should().ftsByProjectRanked(pid.capture(), any(), page.capture(), anyInt(), anyLong());

        assertThat(pid.getValue().projectKey()).isEqualTo("PROJ");
        assertThat(page.getValue()).isEqualTo(2);
        then(issueFtsUseCase).should(never()).ftsAllRanked(any(), anyInt(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("success: state categories are read case-insensitively into the search condition")
    void stateCategoriesAreParsed() {
        authenticate();
        given(issueFtsUseCase.ftsAllRanked(any(), anyInt(), anyInt(), anyLong()))
                .willReturn(Page.empty());

        tool.searchIssues("login", null, List.of("initial", "ACTIVE"), null);

        assertThat(conditionOfGlobalSearch().stateCategories())
                .containsExactlyInAnyOrder(StateCategory.INITIAL, StateCategory.ACTIVE);
    }

    @Test
    @DisplayName("fail: an unknown state category names the valid ones instead of silently widening the search")
    void unknownStateCategoryIsRejected() {
        authenticate();

        assertThatThrownBy(() -> tool.searchIssues("login", null, List.of("DONE"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DONE")
                .hasMessageContaining("INITIAL, ACTIVE, COMPLETED, ABORTED");

        then(issueFtsUseCase).shouldHaveNoInteractions();
    }

    private IssueSearchCondition conditionOfGlobalSearch() {
        ArgumentCaptor<IssueSearchCondition> captor = ArgumentCaptor.forClass(IssueSearchCondition.class);
        then(issueFtsUseCase).should().ftsAllRanked(captor.capture(), anyInt(), anyInt(), anyLong());
        return captor.getValue();
    }

    private void authenticate() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_READ"));
        MemberDetails principal = new MemberDetails(7L, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
