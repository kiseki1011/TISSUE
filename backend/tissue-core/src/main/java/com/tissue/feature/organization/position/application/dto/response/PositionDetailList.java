package com.tissue.feature.organization.position.application.dto.response;

import com.tissue.feature.organization.position.domain.Position;
import java.util.List;

public record PositionDetailList(List<PositionDetail> positions) {
    public static PositionDetailList from(List<Position> positions) {
        List<PositionDetail> responses =
                positions.stream().map(PositionDetail::from).toList();
        return new PositionDetailList(responses);
    }
}
