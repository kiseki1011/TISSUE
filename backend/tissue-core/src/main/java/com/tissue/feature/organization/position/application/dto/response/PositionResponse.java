package com.tissue.feature.organization.position.application.dto.response;

import com.tissue.feature.organization.position.domain.Position;

public record PositionResponse(Long positionId) {

    public static PositionResponse from(Position position) {
        return new PositionResponse(position.getId());
    }
}
