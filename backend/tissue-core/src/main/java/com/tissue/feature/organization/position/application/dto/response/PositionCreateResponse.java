package com.tissue.feature.organization.position.application.dto.response;

import com.tissue.feature.organization.position.domain.Position;

public record PositionCreateResponse(String workspaceKey, Long positionId) {
    public static PositionCreateResponse from(Position position) {
        return new PositionCreateResponse(position.getWorkspaceKey(), position.getId());
    }
}
