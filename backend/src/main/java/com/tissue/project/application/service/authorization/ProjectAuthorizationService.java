package com.tissue.project.application.service.authorization;

import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private final ProjectQueryRepository projectQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final SprintQueryRepository sprintRepository;
    private final IssueTypeQueryRepository issueTypeRepository;
    private final WorkflowQueryRepository workflowQueryRepository;
    private final ProjectMemberQueryRepository projectMemberRepository;

    public void requireProjectViewer(String workspaceKey, String projectKey, Long actorMemberId) {
        if (isViewer(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires project " + ProjectRole.VIEWER.name());
    }

    public void requireProjectMember(String workspaceKey, String projectKey, Long actorMemberId) {
        if (isMember(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires project " + ProjectRole.MEMBER.name());
    }

    public void requireProjectAdmin(String workspaceKey, String projectKey, Long actorMemberId) {
        if (isAdmin(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires project " + ProjectRole.ADMIN.name());
    }

    public void requireDirectJoinPermission(String workspaceKey, String projectKey, Long actorMemberId) {
        if (canJoinViaDirectAccess(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException(
                "If project visibility is not public, requires workspace %s".formatted(WorkspaceRole.ADMIN.name()));
    }

    public void requireRoleGrantPermission(
            String workspaceKey, String projectKey, ProjectRole grantRole, Long actorMemberId) {
        if (canGrantRole(workspaceKey, projectKey, grantRole, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException(
                "Requires workspace %s or an equal or higher role than the project role you are trying to grant"
                        .formatted(WorkspaceRole.ADMIN.name()));
    }

    public void requireSprintEditPermission(String workspaceKey, String projectKey, Long sprintId, Long actorMemberId) {
        if ((isAdmin(workspaceKey, projectKey, actorMemberId)
                || isSprintCreator(projectKey, sprintId, actorMemberId))) {
            return;
        }
        throw new AccessDeniedException(
                "Needs project %s or is the sprint creator".formatted(ProjectRole.ADMIN.name()));
    }

    public void requireIssueTypeEditPermission(
            String workspaceKey, String projectKey, Long issueTypeId, Long actorMemberId) {
        if (isAdmin(workspaceKey, projectKey, actorMemberId)
                || isIssueTypeCreator(projectKey, issueTypeId, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException(
                "Needs project %s or is the issue type creator".formatted(ProjectRole.ADMIN.name()));
    }

    public void requireWorkflowEditPermission(
            String workspaceKey, String projectKey, Long workflowId, Long actorMemberId) {
        if ((isAdmin(workspaceKey, projectKey, actorMemberId) || isWorkflowCreator(workflowId, actorMemberId))) {
            return;
        }
        throw new AccessDeniedException(
                "Needs project %s or is the workflow creator".formatted(ProjectRole.ADMIN.name()));
    }

    public boolean isViewer(String workspaceKey, String projectKey, Long actorMemberId) {
        return hasProjectRole(workspaceKey, projectKey, actorMemberId, ProjectRole.VIEWER);
    }

    public boolean isMember(String workspaceKey, String projectKey, Long actorMemberId) {
        return hasProjectRole(workspaceKey, projectKey, actorMemberId, ProjectRole.MEMBER);
    }

    public boolean isAdmin(String workspaceKey, String projectKey, Long actorMemberId) {
        return hasProjectRole(workspaceKey, projectKey, actorMemberId, ProjectRole.ADMIN);
    }

    public boolean canJoinViaDirectAccess(String workspaceKey, String projectKey, Long actorMemberId) {
        if (workspaceAuthorizationService.isAdmin(workspaceKey, actorMemberId)) {
            return true;
        }
        if (isNotWorkspaceMember(workspaceKey, actorMemberId)) {
            return false;
        }
        return isProjectVisibilityPublic(workspaceKey, projectKey);
    }

    public boolean canGrantRole(String workspaceKey, String projectKey, ProjectRole grantRole, Long actorMemberId) {
        if (!isViewer(workspaceKey, projectKey, actorMemberId)) {
            return false;
        }
        return hasProjectRole(workspaceKey, projectKey, actorMemberId, grantRole);
    }

    private boolean hasProjectRole(
            String workspaceKey, String projectKey, Long actorMemberId, ProjectRole requiredRole) {
        if (workspaceAuthorizationService.isAdmin(workspaceKey, actorMemberId)) {
            return true;
        }
        return projectMemberRepository
                .findRoleByKeysAndMemberId(workspaceKey, projectKey, actorMemberId)
                .map(role -> role.isEqualOrHigherThan(requiredRole))
                .orElse(false);
    }

    private boolean isWorkflowCreator(Long workflowId, Long actorMemberId) {
        return workflowQueryRepository
                .findById(workflowId)
                .map(workflow -> workflow.getCreatedBy().equals(actorMemberId))
                .orElse(false);
    }

    private Boolean isIssueTypeCreator(String projectKey, Long issueTypeId, Long actorMemberId) {
        return issueTypeRepository
                .findByIdAndProjectKey(issueTypeId, projectKey)
                .map(issueType -> issueType.getCreatedBy().equals(actorMemberId))
                .orElse(false);
    }

    private Boolean isSprintCreator(String projectKey, Long sprintId, Long actorMemberId) {
        return sprintRepository
                .findByIdAndProject_Key(sprintId, projectKey)
                .map(sprint -> sprint.getCreatedBy().equals(actorMemberId))
                .orElse(false);
    }

    private boolean isNotWorkspaceMember(String workspaceKey, Long actorMemberId) {
        return !workspaceAuthorizationService.isMember(workspaceKey, actorMemberId);
    }

    private Boolean isProjectVisibilityPublic(String workspaceKey, String projectKey) {
        return projectQueryRepository
                .findVisibilityByKeys(workspaceKey, projectKey)
                .map(visibility -> visibility == ProjectVisibility.PUBLIC)
                .orElse(false);
    }
}
