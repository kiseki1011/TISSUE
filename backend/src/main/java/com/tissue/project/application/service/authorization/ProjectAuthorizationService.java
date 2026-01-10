package com.tissue.project.application.service.authorization;

import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.project.domain.exception.InsufficientProjectRoleException;
import com.tissue.project.domain.exception.ProjectJoinNotAllowedException;
import com.tissue.project.domain.exception.ResourceOwnershipRequiredException;
import com.tissue.project.domain.exception.RoleGrantNotAllowedException;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectQueryRepository projectQueryRepository;
    private final IssueTypeQueryRepository issueTypeRepository;
    private final WorkflowQueryRepository workflowQueryRepository;
    private final ProjectMemberQueryRepository projectMemberRepository;

    public void requireProjectViewer(String workspaceKey, String projectKey, Long actorMemberId) {
        if (isViewer(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new InsufficientProjectRoleException(workspaceKey, projectKey, ProjectRole.VIEWER);
    }

    public void requireProjectMember(String workspaceKey, String projectKey, Long actorMemberId) {
        if (isMember(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new InsufficientProjectRoleException(workspaceKey, projectKey, ProjectRole.MEMBER);
    }

    public void requireProjectAdmin(String workspaceKey, String projectKey, Long actorMemberId) {
        if (isAdmin(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new InsufficientProjectRoleException(workspaceKey, projectKey, ProjectRole.ADMIN);
    }

    public void requireDirectJoinPermission(String workspaceKey, String projectKey, Long actorMemberId) {
        if (canJoinDirectly(workspaceKey, projectKey, actorMemberId)) {
            return;
        }
        throw new ProjectJoinNotAllowedException(workspaceKey, projectKey);
    }

    public void requireRoleGrantPermission(
            String workspaceKey, String projectKey, ProjectRole grantRole, Long actorMemberId) {
        if (canGrantRole(workspaceKey, projectKey, grantRole, actorMemberId)) {
            return;
        }
        throw new RoleGrantNotAllowedException(workspaceKey, projectKey, grantRole);
    }

    public void requireSprintEditPermission(String workspaceKey, String projectKey, Sprint sprint, Long actorMemberId) {
        if ((isAdmin(workspaceKey, projectKey, actorMemberId) || isSprintCreator(sprint, actorMemberId))) {
            return;
        }
        throw new ResourceOwnershipRequiredException(workspaceKey, projectKey, "Sprint");
    }

    public void requireIssueTypeEditPermission(
            String workspaceKey, String projectKey, Long issueTypeId, Long actorMemberId) {
        if (isAdmin(workspaceKey, projectKey, actorMemberId)
                || isIssueTypeCreator(projectKey, issueTypeId, actorMemberId)) {
            return;
        }
        throw new ResourceOwnershipRequiredException(workspaceKey, projectKey, "IssueType");
    }

    public void requireWorkflowEditPermission(
            String workspaceKey, String projectKey, Long workflowId, Long actorMemberId) {
        if ((isAdmin(workspaceKey, projectKey, actorMemberId) || isWorkflowCreator(workflowId, actorMemberId))) {
            return;
        }
        throw new ResourceOwnershipRequiredException(workspaceKey, projectKey, "Workflow");
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

    public boolean canJoinDirectly(String workspaceKey, String projectKey, Long actorMemberId) {
        if (!workspaceAuthorizationService.isMember(workspaceKey, actorMemberId)) {
            return false;
        }
        if (workspaceAuthorizationService.isAdmin(workspaceKey, actorMemberId)) {
            return true;
        }
        return isProjectVisibilityPublic(workspaceKey, projectKey);
    }

    public boolean canGrantRole(String workspaceKey, String projectKey, ProjectRole grantRole, Long actorMemberId) {
        if (!isViewer(workspaceKey, projectKey, actorMemberId)) {
            return false;
        }
        return hasHigherProjectRole(workspaceKey, projectKey, actorMemberId, grantRole);
    }

    // TODO: consider using the ProjectMember entity as the parameter instead of querying the DB
    //   in this case repository might be needed if the service calling this method doesnt retrieve ProjectMember
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

    private boolean hasHigherProjectRole(
            String workspaceKey, String projectKey, Long actorMemberId, ProjectRole requiredRole) {
        if (workspaceAuthorizationService.isAdmin(workspaceKey, actorMemberId)) {
            return true;
        }
        return projectMemberRepository
                .findRoleByKeysAndMemberId(workspaceKey, projectKey, actorMemberId)
                .map(role -> role.isHigherThan(requiredRole))
                .orElse(false);
    }

    // TODO: consider using the Workflow entity as the parameter instead of querying the DB
    private boolean isWorkflowCreator(Long workflowId, Long actorMemberId) {
        return workflowQueryRepository
                .findById(workflowId)
                .map(workflow -> workflow.getCreatedBy().equals(actorMemberId))
                .orElse(false);
    }

    // TODO: consider using the IssueType entity as the parameter instead of querying the DB
    private Boolean isIssueTypeCreator(String projectKey, Long issueTypeId, Long actorMemberId) {
        return issueTypeRepository
                .findByIdAndProjectKey(issueTypeId, projectKey)
                .map(issueType -> issueType.getCreatedBy().equals(actorMemberId))
                .orElse(false);
    }

    private Boolean isSprintCreator(Sprint sprint, Long actorMemberId) {
        return sprint.getCreatedBy().equals(actorMemberId);
    }

    // TODO: consider using the Project entity as the parameter instead of querying the DB
    //  check Visibility in memory
    private Boolean isProjectVisibilityPublic(String workspaceKey, String projectKey) {
        return projectQueryRepository
                .findVisibilityByKeys(workspaceKey, projectKey)
                .map(visibility -> visibility == ProjectVisibility.PUBLIC)
                .orElse(false);
    }
}
