package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.project.application.port.usecase.ProjectMemberQueryUseCase;
import com.tissue.feature.project.application.port.usecase.ProjectQueryUseCase;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ProjectReadToolTest {

    private final ProjectQueryUseCase projectQueryUseCase = mock(ProjectQueryUseCase.class);
    private final ProjectMemberQueryUseCase projectMemberQueryUseCase = mock(ProjectMemberQueryUseCase.class);

    private final ProjectReadTool tool = new ProjectReadTool(projectQueryUseCase, projectMemberQueryUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: list_my_projects is exposed as an MCP tool")
    void registersListMyProjectsAsMcpTool() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("list_my_projects", "get_project_members");
    }

    @Test
    @DisplayName("success: the member roster is read for the named project on the calling agent's behalf")
    void readsTheMemberRosterOfTheNamedProject() {
        authenticate();
        given(projectMemberQueryUseCase.getProjectMembers(any(), any(), any(), any(), anyLong()))
                .willReturn(Page.empty());

        tool.getProjectMembers("PROJ", "kim", null);

        ArgumentCaptor<ProjectIdentifier> pid = ArgumentCaptor.forClass(ProjectIdentifier.class);
        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(projectMemberQueryUseCase)
                .should()
                .getProjectMembers(
                        pid.capture(),
                        org.mockito.ArgumentMatchers.isNull(),
                        keyword.capture(),
                        pageable.capture(),
                        org.mockito.ArgumentMatchers.eq(7L));

        assertThat(pid.getValue().projectKey()).isEqualTo("PROJ");
        assertThat(keyword.getValue()).isEqualTo("kim");
        assertThat(pageable.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("success: the first page excludes archived projects by default")
    void defaultsToFirstPageWithoutArchived() {
        authenticate();
        given(projectQueryUseCase.getMyProjects(anyBoolean(), any(), anyLong())).willReturn(Page.empty());

        tool.listMyProjects(null, null);

        ArgumentCaptor<Boolean> archived = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(projectQueryUseCase).should().getMyProjects(archived.capture(), pageable.capture(), anyLong());

        assertThat(archived.getValue()).isFalse();
        assertThat(pageable.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("success: the requested page and archive setting reach the use case for the calling agent")
    void passesPageAndArchiveThrough() {
        authenticate();
        given(projectQueryUseCase.getMyProjects(anyBoolean(), any(), anyLong())).willReturn(Page.empty());

        tool.listMyProjects(3, true);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        then(projectQueryUseCase)
                .should()
                .getMyProjects(
                        org.mockito.ArgumentMatchers.eq(true), pageable.capture(), org.mockito.ArgumentMatchers.eq(7L));

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(3);
    }

    private void authenticate() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_READ"));
        MemberDetails principal = new MemberDetails(7L, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
