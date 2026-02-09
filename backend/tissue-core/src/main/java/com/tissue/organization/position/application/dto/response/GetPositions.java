package com.tissue.organization.position.application.dto.response;

import com.tissue.organization.position.domain.Position;
import java.util.List;

public record GetPositions(List<PositionDetail> positions) {
    public static GetPositions from(List<Position> positions) {
        List<PositionDetail> responses =
                positions.stream().map(PositionDetail::from).toList();
        return new GetPositions(responses);
    }
}
