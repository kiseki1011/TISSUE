package com.tissue.feature.organization.team.application.dto.response;

import com.tissue.feature.organization.team.domain.Team;
import com.tissue.shared.enums.ColorType;

public record TeamSummary(Long id, String name, String description, ColorType color) {

    public static TeamSummary from(Team team) {
        return new TeamSummary(team.getId(), team.getName(), team.getDescription(), team.getColor());
    }
}
