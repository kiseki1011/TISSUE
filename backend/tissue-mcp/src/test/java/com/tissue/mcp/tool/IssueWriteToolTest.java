package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueLifecycleUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueTransitionUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueUpdateUseCase;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "lgtm",
        model = "claude-opus-4-8")
class IssueWriteToolTest {

    private final IssueLifecycleUseCase lifecycleUseCase = mock(IssueLifecycleUseCase.class);
    private final IssueTransitionUseCase transitionUseCase = mock(IssueTransitionUseCase.class);
    private final IssueParticipantUseCase participantUseCase = mock(IssueParticipantUseCase.class);
    private final CommentCommandUseCase commentCommandUseCase = mock(CommentCommandUseCase.class);
    private final IssueQueryUseCase issueQueryUseCase = mock(IssueQueryUseCase.class);
    private final IssueUpdateUseCase issueUpdateUseCase = mock(IssueUpdateUseCase.class);
    private final ConversionService conversionService = mock(ConversionService.class);

    private final IssueWriteTool tool = new IssueWriteTool(
            lifecycleUseCase,
            transitionUseCase,
            participantUseCase,
            commentCommandUseCase,
            issueQueryUseCase,
            issueUpdateUseCase,
            conversionService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: the write methods are exposed as MCP tools")
    void registersWriteMethodsAsMcpTools() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains(
                        "create_issue",
                        "update_issue",
                        "transition_issue",
                        "claim_issue",
                        "assign_issue",
                        "add_comment");
    }

    @Test
    @DisplayName("fail: update_issue with no fields to change throws an actionable error")
    void updateIssueRequiresAtLeastOneField() {
        authenticate(7L, "SCOPE_READ", "SCOPE_WRITE");

        assertThatThrownBy(() -> tool.updateIssue("PROJ-1", null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);

        then(issueUpdateUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("fail: a write tool throws AccessDeniedException when the PAT lacks SCOPE_WRITE")
    void rejectsWriteWithoutWriteScope() {
        authenticate(7L, "SCOPE_READ");

        assertThatThrownBy(() -> tool.claimIssue("PROJ-1")).isInstanceOf(AccessDeniedException.class);

        then(participantUseCase).should(never()).claim(any(), any());
    }

    @Test
    @DisplayName("success: a write tool delegates to the use case when the PAT carries SCOPE_WRITE")
    void allowsWriteWithWriteScope() {
        authenticate(7L, "SCOPE_READ", "SCOPE_WRITE");

        tool.claimIssue("PROJ-1");

        then(participantUseCase).should().claim(IssueIdentifier.ofIssueKey("PROJ-1"), 7L);
        then(issueQueryUseCase).should().getDetail(IssueIdentifier.ofIssueKey("PROJ-1"), 7L);
    }

    private void authenticate(Long memberId, String... scopes) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(scopes).map(SimpleGrantedAuthority::new).toList();
        MemberDetails principal = new MemberDetails(memberId, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
