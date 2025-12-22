package com.tissue.team.application.dto.response;

import com.tissue.common.enums.ColorType;
import com.tissue.team.domain.Team;

import lombok.Builder;

@Builder
public record TeamDetail(
	String workspaceKey,
	Long teamId,
	String name,
	String description,
	ColorType color
) {
	public static TeamDetail from(Team team) {
		return TeamDetail.builder()
			.workspaceKey(team.getWorkspaceKey())
			.teamId(team.getId())
			.name(team.getDisplayName())
			.description(team.getDescription())
			.color(team.getColor())
			.build();
	}
}
