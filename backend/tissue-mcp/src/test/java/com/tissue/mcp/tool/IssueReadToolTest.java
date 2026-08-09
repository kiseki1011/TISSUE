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

import com.tissue.feature.comment.application.dto.response.CommentAuthorInfo;
import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.issue.application.dto.request.IssueDetailSection;
import com.tissue.feature.issue.application.dto.response.IssueBranchView;
import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetailView;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.port.usecase.IssueDetailViewUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.PageResponse;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class IssueReadToolTest {

    private final IssueQueryUseCase issueQueryUseCase = mock(IssueQueryUseCase.class);
    private final IssueDetailViewUseCase issueDetailViewUseCase = mock(IssueDetailViewUseCase.class);

    private final IssueReadTool tool = new IssueReadTool(issueQueryUseCase, issueDetailViewUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: the read methods are exposed as MCP tools")
    void registersReadMethodsAsMcpTools() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("get_issue", "list_available_transitions");
    }

    @Test
    @DisplayName("success: get_issue without include asks for no extra section and returns none")
    void getIssueWithoutIncludeReturnsTheIssueAlone() {
        authenticate();
        given(issueDetailViewUseCase.getDetailView(any(), any(), anyInt(), anyLong()))
                .willReturn(fullView());

        IssueReadTool.IssueView result = tool.getIssue("PROJ-1", null);

        assertThat(sectionsAskedFor()).isEmpty();
        assertThat(result.common().issueKey()).isEqualTo("PROJ-1");
        assertThat(result.comments()).isNull();
        assertThat(result.relations()).isNull();
        assertThat(result.children()).isNull();
        assertThat(result.parent()).isNull();
        assertThat(result.branches()).isNull();
        assertThat(result.pullRequests()).isNull();
    }

    @Test
    @DisplayName("success: only the requested sections are asked for and carried back")
    void getIssueReturnsOnlyTheRequestedSections() {
        authenticate();
        given(issueDetailViewUseCase.getDetailView(any(), any(), anyInt(), anyLong()))
                .willReturn(fullView());

        IssueReadTool.IssueView result = tool.getIssue("PROJ-1", List.of("comments", "vcs"));

        assertThat(sectionsAskedFor()).containsExactlyInAnyOrder(IssueDetailSection.COMMENTS, IssueDetailSection.VCS);
        assertThat(result.comments()).isNotNull();
        assertThat(result.branches()).isNotNull();
        assertThat(result.pullRequests()).isNotNull();
        assertThat(result.relations()).isNull();
        assertThat(result.children()).isNull();
    }

    @Test
    @DisplayName("success: include is read case-insensitively and ignores surrounding blanks")
    void includeIsForgivingAboutCasing() {
        authenticate();
        given(issueDetailViewUseCase.getDetailView(any(), any(), anyInt(), anyLong()))
                .willReturn(fullView());

        tool.getIssue("PROJ-1", List.of(" Hierarchy ", "RELATIONS"));

        assertThat(sectionsAskedFor())
                .containsExactlyInAnyOrder(IssueDetailSection.HIERARCHY, IssueDetailSection.RELATIONS);
    }

    @Test
    @DisplayName("fail: an unknown include section names the valid ones instead of being ignored")
    void unknownIncludeSectionIsRejected() {
        authenticate();

        assertThatThrownBy(() -> tool.getIssue("PROJ-1", List.of("attachments")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attachments")
                .hasMessageContaining("comments, relations, hierarchy, vcs");

        then(issueDetailViewUseCase).should(never()).getDetailView(any(), any(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("success: an issue with no parent reports none rather than an empty parent")
    void parentlessIssueReportsNoParent() {
        authenticate();
        given(issueDetailViewUseCase.getDetailView(any(), any(), anyInt(), anyLong()))
                .willReturn(viewWithParent(IssueIdentifierResponse.asNull()));

        IssueReadTool.IssueView result = tool.getIssue("PROJ-1", List.of("hierarchy"));

        assertThat(result.parent()).isNull();
        assertThat(result.children()).isNotNull();
    }

    @Test
    @DisplayName("success: list_available_transitions delegates for the calling agent")
    void listAvailableTransitionsDelegates() {
        authenticate();

        tool.listAvailableTransitions("PROJ-1");

        then(issueQueryUseCase).should().getAvailableTransitions(IssueIdentifier.ofIssueKey("PROJ-1"), 7L);
    }

    private Set<IssueDetailSection> sectionsAskedFor() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<IssueDetailSection>> captor = ArgumentCaptor.forClass(Set.class);
        then(issueDetailViewUseCase).should().getDetailView(any(), captor.capture(), anyInt(), anyLong());
        return captor.getValue();
    }

    /** A view with every section populated, so a null on the way out can only come from the section filter. */
    private static IssueDetailView fullView() {
        return viewWithParent(new IssueIdentifierResponse("PROJ-9", null, null));
    }

    private static IssueDetailView viewWithParent(IssueIdentifierResponse parent) {
        CommentDetailResponse comment = new CommentDetailResponse(
                1L,
                "looks good",
                false,
                false,
                null,
                Instant.EPOCH,
                Instant.EPOCH,
                new CommentAuthorInfo(2L, "agent", "Agent"),
                new ArrayList<>());

        return new IssueDetailView(
                IssueCommonDetail.builder().issueKey("PROJ-1").build(),
                List.of(),
                List.of(),
                parent,
                List.of(new IssueIdentifierResponse("PROJ-2", null, null)),
                IssueRelationsDetail.empty(),
                PageResponse.from(new PageImpl<>(List.of(comment))),
                List.of(new IssueBranchView(
                        "https://example.test/repo",
                        "feature/PROJ-1",
                        "https://example.test/repo/tree/feature/PROJ-1",
                        null,
                        null,
                        null,
                        null,
                        null)),
                List.of());
    }

    private void authenticate() {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(new String[] {"SCOPE_READ"})
                .map(SimpleGrantedAuthority::new)
                .toList();
        MemberDetails principal = new MemberDetails(7L, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
