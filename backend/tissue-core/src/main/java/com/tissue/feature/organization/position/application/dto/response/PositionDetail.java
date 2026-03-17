package com.tissue.feature.organization.position.application.dto.response;

import com.tissue.feature.organization.position.domain.Position;
import com.tissue.shared.enums.ColorType;
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
                .name(position.getName().getDisplayName())
                .description(position.getDescription())
                .color(position.getColor())
                .build();
    }
}
