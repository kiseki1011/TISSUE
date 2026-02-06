package com.tissue.organization.position.application.dto.response;

import com.tissue.organization.position.domain.Position;

public record PositionCreateResponse(String workspaceCode, Long positionId) {
    public static PositionCreateResponse from(Position position) {
        return new PositionCreateResponse(position.getWorkspaceKey(), position.getId());
    }
}
