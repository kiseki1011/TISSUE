package com.tissue.project.application.service.authorization;

import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.exception.InsufficientProjectRoleException;
import com.tissue.project.domain.exception.ProjectJoinNotAllowedException;
import com.tissue.project.domain.exception.ResourceOwnershipRequiredException;
import com.tissue.project.domain.exception.RoleGrantNotAllowedException;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    public void requireProjectViewer(ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectViewer()) {
            return;
        }
        throw new InsufficientProjectRoleException(actor.workspaceKey(), actor.projectKey(), ProjectRole.VIEWER);
    }

    public void requireProjectMember(ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectMember()) {
            return;
        }
        throw new InsufficientProjectRoleException(actor.workspaceKey(), actor.projectKey(), ProjectRole.MEMBER);
    }

    public void requireProjectAdmin(ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectAdmin()) {
            return;
        }
        throw new InsufficientProjectRoleException(actor.workspaceKey(), actor.projectKey(), ProjectRole.ADMIN);
    }

    public void requireDirectJoinPermission(WorkspaceMemberContext actor, Project project) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (project.isPublic()) {
            return;
        }
        throw new ProjectJoinNotAllowedException(actor.workspaceKey(), project.getKey());
    }

    public void requireRoleGrantPermission(ProjectMemberContext actor, ProjectMember target, ProjectRole grantRole) {
        boolean touchesAdminRole = target.getRole().isAdmin() || grantRole.isAdmin();

        if (touchesAdminRole) {
            if (!actor.isWorkspaceAdmin()) {
                throw new RoleGrantNotAllowedException(actor.workspaceRole(), actor.projectRole());
            }
            return;
        }

        if (actor.isProjectAdmin() || actor.isWorkspaceAdmin()) {
            return;
        }

        throw new RoleGrantNotAllowedException(actor.workspaceRole(), actor.projectRole());
    }

    public void requireSprintEditPermission(ProjectMemberContext actor, Sprint sprint) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectAdmin()) {
            return;
        }
        if (isSprintCreator(sprint, actor.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.workspaceKey(), actor.projectKey(), "Sprint");
    }

    public void requireIssueTypeEditPermission(ProjectMemberContext actor, IssueType issueType) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectAdmin()) {
            return;
        }
        if (isIssueTypeCreator(issueType, actor.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.workspaceKey(), actor.projectKey(), "IssueType");
    }

    public void requireWorkflowEditPermission(ProjectMemberContext actor, Workflow workflow) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectAdmin()) {
            return;
        }
        if (isWorkflowCreator(workflow, actor.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.workspaceKey(), actor.projectKey(), "Workflow");
    }

    private boolean isWorkflowCreator(Workflow workflow, Long actorMemberId) {
        return workflow.getCreatedBy().equals(actorMemberId);
    }

    private Boolean isIssueTypeCreator(IssueType issueType, Long actorMemberId) {
        return issueType.getCreatedBy().equals(actorMemberId);
    }

    private Boolean isSprintCreator(Sprint sprint, Long actorMemberId) {
        return sprint.getCreatedBy().equals(actorMemberId);
    }
}
