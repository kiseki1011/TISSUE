package com.tissue.project.application.service.authorization;

import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectJoinNotAllowedException;
import com.tissue.project.domain.exception.ProjectMemberNotFoundException;
import com.tissue.project.domain.exception.RequireProjectEditPermission;
import com.tissue.project.domain.exception.ResourceOwnershipRequiredException;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
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

    public void requireProjectEditPermission(WorkspaceMemberContext actorContext, Project project) {
        if (actorContext.isWorkspaceAdmin()) {
            return;
        }
        if (isProjectCreator(project, actorContext.memberId())) {
            return;
        }
        throw new RequireProjectEditPermission(actorContext.workspaceKey(), project.getKey());
    }

    public void requireJoinPermission(WorkspaceMemberContext actorContext, Project project) {
        if (actorContext.isWorkspaceAdmin()) {
            return;
        }
        if (project.isPublic()) {
            return;
        }
        throw new ProjectJoinNotAllowedException(actorContext.workspaceKey(), project.getKey());
    }

    // TODO: ResourceOwnershipRequiredException -> SprintOwnershipRequiredException
    public void requireSprintEditPermission(ProjectMemberContext actorContext, Sprint sprint) {
        if (actorContext.isWorkspaceAdmin()) {
            return;
        }
        if (isSprintCreator(sprint, actorContext.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(actorContext.workspaceKey(), actorContext.projectKey(), "Sprint");
    }

    // TODO: ResourceOwnershipRequiredException -> IssueTypeOwnershipRequiredException
    public void requireIssueTypeEditPermission(ProjectMemberContext actorContext, IssueType issueType) {
        if (actorContext.isWorkspaceAdmin()) {
            return;
        }
        if (isIssueTypeCreator(issueType, actorContext.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(
                actorContext.workspaceKey(), actorContext.projectKey(), "IssueType");
    }

    // TODO: ResourceOwnershipRequiredException -> WorkflowOwnershipRequiredException
    public void requireWorkflowEditPermission(ProjectMemberContext actorContext, Workflow workflow) {
        if (actorContext.isWorkspaceAdmin()) {
            return;
        }
        if (isWorkflowCreator(workflow, actorContext.memberId())) {
            return;
        }
        throw new ResourceOwnershipRequiredException(
                actorContext.workspaceKey(), actorContext.projectKey(), "Workflow");
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
