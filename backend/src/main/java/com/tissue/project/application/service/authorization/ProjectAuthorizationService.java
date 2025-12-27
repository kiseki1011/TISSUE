package com.tissue.project.application.service.authorization;

import org.springframework.stereotype.Component;

import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectAuthorizationService {

	private final ProjectQueryRepository projectQueryRepository;
	private final WorkspaceAuthorizationService workspaceAuthorizationService;
	private final SprintQueryRepository sprintRepository;
	private final IssueTypeQueryRepository issueTypeRepository;
	private final WorkflowQueryRepository workflowQueryRepository;

	public boolean isViewer(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		return userDetails.hasProjectRole(workspaceKey, projectKey, ProjectRole.VIEWER);
	}

	public boolean isMember(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		return userDetails.hasProjectRole(workspaceKey, projectKey, ProjectRole.MEMBER);
	}

	public boolean isAdmin(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		return userDetails.hasProjectRole(workspaceKey, projectKey, ProjectRole.ADMIN);
	}

	public boolean canJoinViaDirectAccess(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		if (workspaceAuthorizationService.isAdmin(workspaceKey, userDetails)) {
			return true;
		}
		if (isNotWorkspaceMember(workspaceKey, userDetails)) {
			return false;
		}
		return isProjectVisibilityPublic(workspaceKey, projectKey);
	}

	public boolean canGrantRole(String workspaceKey, String projectKey, ProjectRole grantRole,
		MemberUserDetails userDetails) {
		if (!isViewer(workspaceKey, projectKey, userDetails)) {
			return false;
		}
		return userDetails.hasProjectRole(workspaceKey, projectKey, grantRole);
	}

	public boolean canEditSprint(String workspaceKey, String projectKey, Long sprintId,
		MemberUserDetails userDetails) {
		return isAdmin(workspaceKey, projectKey, userDetails)
			|| isSprintCreator(projectKey, sprintId, userDetails);
	}

	public boolean canEditIssueType(String workspaceKey, String projectKey, Long issueTypeId,
		MemberUserDetails userDetails) {
		return isAdmin(workspaceKey, projectKey, userDetails)
			|| isIssueTypeCreator(projectKey, issueTypeId, userDetails);
	}

	public boolean canEditWorkflow(String workspaceKey, String projectKey, Long workflowId,
		MemberUserDetails userDetails) {
		return isAdmin(workspaceKey, projectKey, userDetails)
			|| isWorkflowCreator(workflowId, userDetails);
	}

	private boolean isWorkflowCreator(Long workflowId, MemberUserDetails userDetails) {
		return workflowQueryRepository.findById(workflowId)
			.map(workflow -> workflow.getCreatedBy().equals(userDetails.getMemberId()))
			.orElse(false);
	}

	private Boolean isIssueTypeCreator(String projectKey, Long issueTypeId, MemberUserDetails userDetails) {
		return issueTypeRepository.findByIdAndProjectKey(issueTypeId, projectKey)
			.map(issueType -> issueType.getCreatedBy().equals(userDetails.getMemberId()))
			.orElse(false);
	}

	private Boolean isSprintCreator(String projectKey, Long sprintId, MemberUserDetails userDetails) {
		return sprintRepository.findByIdAndProject_Key(sprintId, projectKey)
			.map(sprint -> sprint.getCreatedBy().equals(userDetails.getMemberId()))
			.orElse(false);
	}

	private boolean isNotWorkspaceMember(String workspaceKey, MemberUserDetails userDetails) {
		return !workspaceAuthorizationService.isMember(workspaceKey, userDetails);
	}

	private Boolean isProjectVisibilityPublic(String workspaceKey, String projectKey) {
		return projectQueryRepository.findVisibilityByKeys(workspaceKey, projectKey)
			.map(visibility -> visibility == ProjectVisibility.PUBLIC)
			.orElse(false);
	}
}
