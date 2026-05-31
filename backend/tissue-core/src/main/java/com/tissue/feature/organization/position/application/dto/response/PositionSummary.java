package com.tissue.feature.organization.position.application.dto.response;

import com.tissue.feature.organization.position.domain.Position;
import com.tissue.shared.enums.ColorType;

public record PositionSummary(Long id, String name, String description, ColorType color) {

    public static PositionSummary from(Position position) {
        return new PositionSummary(
                position.getId(), position.getName(), position.getDescription(), position.getColor());
    }
}
