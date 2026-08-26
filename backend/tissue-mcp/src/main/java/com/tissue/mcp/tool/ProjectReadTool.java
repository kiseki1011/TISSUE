package com.tissue.mcp.tool;

import com.tissue.feature.project.application.dto.response.ProjectMemberSummary;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.usecase.ProjectMemberQueryUseCase;
import com.tissue.feature.project.application.port.usecase.ProjectQueryUseCase;
import com.tissue.shared.dto.PageResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectReadTool {

    private static final int PAGE_SIZE = 50;

    private final ProjectQueryUseCase projectQueryUseCase;
    private final ProjectMemberQueryUseCase projectMemberQueryUseCase;

    @McpTool(name = "list_my_projects", description = """
            List the projects you belong to, with the project key each of the other tools needs. Start \
            here when you do not already know which project to work in: every other project is closed to \
            you, so nothing outside this list is worth trying. Each entry carries the key, title, your \
            role in it, and when it last saw activity. If hasNext is true, pass the next page number as \
            the page argument.""")
    public PageResponse<ProjectSummary> listMyProjects(
            @McpToolParam(required = false, description = "Zero-based page number. Omit for the first page.") @Nullable
                    Integer page,
            @McpToolParam(
                            required = false,
                            description = "Include archived projects. Archived projects are kept for "
                                    + "reference and are not where active work happens. Defaults to false.")
                    @Nullable
                    Boolean includeArchived) {
        return PageResponse.from(projectQueryUseCase.getMyProjects(
                Boolean.TRUE.equals(includeArchived),
                PageRequest.of(page == null ? 0 : page, PAGE_SIZE),
                McpActor.currentMemberId()));
    }

    @McpTool(name = "get_project_members", description = """
            List the people and agents in a project, with the memberId that assign_issue and \
            request_review need. Each entry carries the memberId, username, display name, project role \
            (MEMBER or MANAGER), and whether it is a human or an agent - with the owning user's name for \
            an agent. Call this to turn a name into the id an action needs, or to see who is available \
            to take work. If hasNext is true, pass the next page number as the page argument.""")
    public PageResponse<ProjectMemberSummary> getProjectMembers(
            @McpToolParam(required = true, description = "The project key, ex: \"PROJ\".") String projectKey,
            @McpToolParam(required = false, description = "Filter by username or display name. Omit to list everyone.")
                    @Nullable
                    String keyword,
            @McpToolParam(required = false, description = "Zero-based page number. Omit for the first page.") @Nullable
                    Integer page) {
        return PageResponse.from(projectMemberQueryUseCase.getProjectMembers(
                ProjectIdentifier.ofProjectKey(projectKey),
                null,
                keyword,
                PageRequest.of(page == null ? 0 : page, PAGE_SIZE),
                McpActor.currentMemberId()));
    }
}
