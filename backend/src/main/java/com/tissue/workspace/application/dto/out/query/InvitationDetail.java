package com.tissue.workspace.application.dto.out.query;

import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.member.domain.Member;
import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.enums.InvitationStatus;

import lombok.Builder;

@Builder
public record InvitationDetail(
	Long invitationId,
	String workspaceKey,
	String workspaceName,
	List<ProjectJoinConfigDto> projectConfigs,
	String inviterName,
	String inviterEmail,
	InvitationStatus status,
	Instant invitedAt
) {
	public static InvitationDetail from(Invitation invitation, @Nullable Member inviter) {
		String name = (inviter != null) ? inviter.getName() : "UNKNOWN";
		String email = (inviter != null) ? inviter.getEmail() : "";

		return InvitationDetail.builder()
			.invitationId(invitation.getId())
			.workspaceKey(invitation.getWorkspaceKey())
			.workspaceName(invitation.getWorkspace().getName())
			.projectConfigs(
				invitation.getProjectConfigs().stream()
					.map(config -> new ProjectJoinConfigDto(
						config.projectKey(),
						config.role()
					))
					.toList()
			)
			.inviterName(name)
			.inviterEmail(email)
			.status(invitation.getStatus())
			.invitedAt(invitation.getCreatedAt())
			.build();
	}
}
