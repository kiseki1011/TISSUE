package com.tissue.feature.project.application.service.authorization;

import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectJoinNotAllowedException;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.feature.project.domain.exception.RequireProjectEditPermission;
import com.tissue.feature.project.domain.exception.RequireProjectManagerException;
import com.tissue.feature.project.domain.exception.ResourceOwnershipRequiredException;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private final ProjectMemberQueryRepository projectMemberQueryRepository;

    public void requireProjectMember(Project targetProject, Long memberId) {
        boolean hasTargetAccess = projectMemberQueryRepository.existsByProjectAndMemberId(targetProject, memberId);
        if (!hasTargetAccess) {
            throw new ProjectMemberNotFoundException(targetProject.getWorkspaceKey(), targetProject.getKey(), memberId);
        }
    }

    // TODO: should i change this to check isProjectManager() instead of isProjectCreator?
    public void requireProjectEditPermission(WorkspaceMemberContext actor, Project project) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isProjectCreator(project, actor.memberId())) {
            return;
        }
        throw new RequireProjectEditPermission(actor.workspaceKey(), project.getKey());
    }

    public void requireProjectManager(ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (actor.isProjectManager()) {
            return;
        }
        throw new RequireProjectManagerException(actor.workspaceKey(), actor.projectKey());
    }

    public void requireJoinPermission(WorkspaceMemberContext actor, Project project) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (project.isPublic()) {
            return;
        }
        throw new ProjectJoinNotAllowedException(actor.workspaceKey(), project.getKey());
    }

    // TODO: ResourceOwnershipRequiredException -> SprintOwnershipRequiredException
    public void requireSprintEditPermission(ProjectMemberContext actor, Sprint sprint) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isSprintCreator(sprint, actor.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.workspaceKey(), actor.projectKey(), "Sprint");
    }

    // TODO: ResourceOwnershipRequiredException -> IssueTypeOwnershipRequiredException
    public void requireIssueTypeEditPermission(ProjectMemberContext actor, IssueType issueType) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isIssueTypeCreator(issueType, actor.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.workspaceKey(), actor.projectKey(), "IssueType");
    }

    // TODO: ResourceOwnershipRequiredException -> WorkflowOwnershipRequiredException
    public void requireWorkflowEditPermission(ProjectMemberContext actor, Workflow workflow) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isWorkflowCreator(workflow, actor.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actor.workspaceKey(), actor.projectKey(), "Workflow");
    }

    private boolean isProjectCreator(Project project, Long actorMemberId) {
        return project.getCreatedBy().equals(actorMemberId);
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
