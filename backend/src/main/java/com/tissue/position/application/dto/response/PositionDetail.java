package com.tissue.position.application.dto.response;

import com.tissue.common.enums.ColorType;
import com.tissue.position.domain.Position;
import lombok.Builder;

@Builder
public record PositionDetail(String workspaceKey, Long positionId, String name, String description, ColorType color) {
    public static PositionDetail from(Position position) {
        return PositionDetail.builder()
                .workspaceKey(position.getWorkspaceKey())
                .positionId(position.getId())
                .name(position.getName().getDisplay())
                .description(position.getDescription())
                .color(position.getColor())
                .build();
    }
}
