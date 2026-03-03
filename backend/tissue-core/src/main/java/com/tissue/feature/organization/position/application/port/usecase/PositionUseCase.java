package com.tissue.feature.organization.position.application.port.usecase;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.UpdatePositionCommand;
import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;

public interface PositionUseCase {

    PositionCreateResponse create(String workspaceKey, CreatePositionCommand cmd, Long actorMemberId);

    void update(String workspaceKey, Long positionId, UpdatePositionCommand cmd, Long actorMemberId);

    void delete(String workspaceKey, Long positionId, Long actorMemberId);

    PositionDetail getPosition(String workspaceKey, Long positionId, Long actorMemberId);

    PositionDetailList getWorkspacePositions(String workspaceKey, Long actorMemberId);
}
