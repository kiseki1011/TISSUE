package com.tissue.api.workspacemember.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.tissue.api.team.presentation.dto.response.TeamDetail;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

public record AssignTeamResponse(
	Long workspaceMemberId,
	List<TeamDetail> assignedTeams,
	LocalDateTime assignedAt
) {
	public static AssignTeamResponse from(WorkspaceMember workspaceMember) {
		List<TeamDetail> teamDetails = workspaceMember.getWorkspaceMemberTeams().stream()
			.map(wmp -> TeamDetail.from(wmp.getTeam()))
			.toList();

		return new AssignTeamResponse(
			workspaceMember.getId(),
			teamDetails,
			workspaceMember.getLastModifiedDate()
		);
	}
}
