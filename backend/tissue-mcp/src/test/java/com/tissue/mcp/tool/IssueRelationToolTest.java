package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.port.usecase.IssueRelationUseCase;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class IssueRelationToolTest {

    private final IssueRelationUseCase issueRelationUseCase = mock(IssueRelationUseCase.class);

    private final IssueRelationTool tool = new IssueRelationTool(issueRelationUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: the relation methods are exposed as MCP tools")
    void registersRelationMethodsAsMcpTools() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("add_relation", "remove_relation");
    }

    @Test
    @DisplayName("success: the named issue is the source and the target keeps its place")
    void addRelationKeepsTheDirectionAsGiven() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.addRelation("PROJ-1", "PROJ-2", IssueRelationType.BLOCKS);

        then(issueRelationUseCase)
                .should()
                .add(IssueIdentifier.ofIssueKey("PROJ-1"), "PROJ-2", IssueRelationType.BLOCKS, 7L);
    }

    @Test
    @DisplayName("success: removal identifies the relation by the target alone")
    void removeRelationNeedsNoRelationType() {
        authenticate("SCOPE_READ", "SCOPE_WRITE");

        tool.removeRelation("PROJ-1", "PROJ-2");

        then(issueRelationUseCase).should().remove(IssueIdentifier.ofIssueKey("PROJ-1"), "PROJ-2", 7L);
    }

    @Test
    @DisplayName("fail: adding a relation needs a READ_WRITE token")
    void addRelationRequiresWriteScope() {
        authenticate("SCOPE_READ");

        assertThatThrownBy(() -> tool.addRelation("PROJ-1", "PROJ-2", IssueRelationType.BLOCKS))
                .isInstanceOf(AccessDeniedException.class);

        then(issueRelationUseCase).should(never()).add(any(), anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("fail: removing a relation needs a READ_WRITE token")
    void removeRelationRequiresWriteScope() {
        authenticate("SCOPE_READ");

        assertThatThrownBy(() -> tool.removeRelation("PROJ-1", "PROJ-2")).isInstanceOf(AccessDeniedException.class);

        then(issueRelationUseCase).should(never()).remove(any(), anyString(), anyLong());
    }

    private void authenticate(String... scopes) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(scopes).map(SimpleGrantedAuthority::new).toList();
        MemberDetails principal = new MemberDetails(7L, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
