package com.tissue.organization.position.application.dto.response;

import com.tissue.enums.ColorType;
import com.tissue.organization.position.domain.Position;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record PositionDetail(
        String workspaceKey,
        Long positionId,
        String name,
        @Nullable String description,
        ColorType color) {

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
