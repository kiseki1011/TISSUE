package com.tissue.api.workspace.application.dto.response.query;

import java.util.List;

import com.tissue.api.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.api.workspace.domain.WorkspaceInviteLink;
import com.tissue.api.workspace.domain.WorkspaceMember;

import lombok.Builder;

@Builder
public record WorkspaceInviteLinkDetail(
	String workspaceKey,
	String workspaceName,
	List<ProjectJoinConfigDto> projectConfigs,
	String creatorDisplayName,
	String creatorEmail,
	boolean isValid
) {
	// TODO: 성능 문제는 없을까? dto 프로젝션 사용을 고려할까?
	public static WorkspaceInviteLinkDetail of(WorkspaceInviteLink link, WorkspaceMember linkCreator) {
		return WorkspaceInviteLinkDetail.builder()
			.workspaceKey(link.getWorkspaceKey())
			.workspaceName(link.getWorkspace().getName())
			.projectConfigs(
				link.getProjectConfigs().stream()
					.map(config -> new ProjectJoinConfigDto(
						config.projectKey(),
						config.role()
					))
					.toList()
			)
			.creatorDisplayName(linkCreator.getDisplayName())
			.creatorEmail(linkCreator.getEmail())
			.isValid(link.isValid())
			.build();
	}
}
