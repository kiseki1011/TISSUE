package com.tissue.feature.organization.position.application.port.usecase;

import com.tissue.feature.organization.position.application.dto.response.PositionSummary;
import java.util.List;

public interface PositionQueryUseCase {

    List<PositionSummary> getPositions(Long actorMemberId);
}
