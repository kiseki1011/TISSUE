package com.tissue.position.application.dto.response;

import com.tissue.position.domain.Position;
import java.util.List;

public record GetPositions(List<PositionDetail> positions) {
    public static GetPositions from(List<Position> positions) {
        List<PositionDetail> responses = positions.stream().map(PositionDetail::from).toList();
        return new GetPositions(responses);
    }
}
