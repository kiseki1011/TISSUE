package com.tissue.team.application.dto.response;

import com.tissue.common.enums.ColorType;
import com.tissue.team.domain.Team;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record TeamDetail(
        String workspaceKey,
        Long teamId,
        String name,
        @Nullable String description,
        ColorType color) {
    public static TeamDetail from(Team team) {
        return TeamDetail.builder()
                .workspaceKey(team.getWorkspaceKey())
                .teamId(team.getId())
                .name(team.getName().toString())
                .description(team.getDescription())
                .color(team.getColor())
                .build();
    }
}
