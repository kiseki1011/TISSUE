package com.tissue.feature.organization.position.application.port.usecase;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.PatchPositionCommand;
import com.tissue.feature.organization.position.application.dto.response.PositionResponse;

public interface PositionUseCase {

    PositionResponse create(CreatePositionCommand cmd, Long actorMemberId);

    void update(Long positionId, PatchPositionCommand cmd, Long actorMemberId);

    void delete(Long positionId, Long actorMemberId);
}
