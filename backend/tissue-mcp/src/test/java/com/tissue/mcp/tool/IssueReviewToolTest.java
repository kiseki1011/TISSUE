package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class IssueReviewToolTest {

    private final IssueReviewUseCase issueReviewUseCase = mock(IssueReviewUseCase.class);
    private final IssueParticipantUseCase issueParticipantUseCase = mock(IssueParticipantUseCase.class);

    private final IssueReviewTool tool = new IssueReviewTool(issueReviewUseCase, issueParticipantUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: the review methods are exposed as MCP tools")
    void registersReviewMethodsAsMcpTools() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("add_reviewer", "remove_reviewer", "submit_review", "request_review");
    }

    @Test
    @DisplayName("success: a reviewer is added to the named issue on the calling agent's behalf")
    void addReviewerDelegates() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.addReviewer("PROJ-1", 42L);

        then(issueParticipantUseCase).should().addReviewer(IssueIdentifier.ofIssueKey("PROJ-1"), 42L, 7L);
    }

    @Test
    @DisplayName("success: a reviewer is removed from the named issue on the calling agent's behalf")
    void removeReviewerDelegates() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.removeReviewer("PROJ-1", 42L);

        then(issueParticipantUseCase).should().removeReviewer(IssueIdentifier.ofIssueKey("PROJ-1"), 42L, 7L);
    }

    @Test
    @DisplayName("fail: changing the reviewer list needs a READ_WRITE token")
    void reviewerRosterRequiresWriteScope() {
        authenticate("SCOPE_READ");

        assertThatThrownBy(() -> tool.addReviewer("PROJ-1", 42L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tool.removeReviewer("PROJ-1", 42L)).isInstanceOf(AccessDeniedException.class);

        then(issueParticipantUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("success: a verdict and its feedback body reach the use case together")
    void submitReviewPassesVerdictAndFeedback() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.submitReview("PROJ-1", false, "rename the method");

        then(issueReviewUseCase)
                .should()
                .submitReview(IssueIdentifier.ofIssueKey("PROJ-1"), false, "rename the method", 7L);
    }

    @Test
    @DisplayName("success: approving with nothing to say submits no feedback body")
    void submitReviewWithoutFeedback() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.submitReview("PROJ-1", true, null);

        then(issueReviewUseCase).should().submitReview(IssueIdentifier.ofIssueKey("PROJ-1"), true, null, 7L);
    }

    @Test
    @DisplayName("fail: submitting a review needs a READ_WRITE token")
    void submitReviewRequiresWriteScope() {
        authenticate("SCOPE_READ");

        assertThatThrownBy(() -> tool.submitReview("PROJ-1", true, null)).isInstanceOf(AccessDeniedException.class);

        then(issueReviewUseCase).should(never()).submitReview(any(), anyBoolean(), any(), anyLong());
    }

    @Test
    @DisplayName("success: naming no reviewer asks everyone who requested changes")
    void requestReviewWithoutIdsTargetsEveryone() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.requestReview("PROJ-1", null);

        then(issueReviewUseCase).should().requestReview(IssueIdentifier.ofIssueKey("PROJ-1"), Set.of(), 7L);
    }

    @Test
    @DisplayName("success: named reviewers reach the use case as a set")
    void requestReviewPassesNamedReviewers() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.requestReview("PROJ-1", List.of(2L, 3L, 2L));

        then(issueReviewUseCase).should().requestReview(IssueIdentifier.ofIssueKey("PROJ-1"), Set.of(2L, 3L), 7L);
    }

    @Test
    @DisplayName("fail: requesting a re-review needs a READ_WRITE token")
    void requestReviewRequiresWriteScope() {
        authenticate("SCOPE_READ");

        assertThatThrownBy(() -> tool.requestReview("PROJ-1", null)).isInstanceOf(AccessDeniedException.class);

        then(issueReviewUseCase).should(never()).requestReview(any(), any(), anyLong());
    }

    private void authenticate(String... scopes) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(scopes).map(SimpleGrantedAuthority::new).toList();
        MemberDetails principal = new MemberDetails(7L, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
