package com.tissue.feature.organization.position.application.port.usecase;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.UpdatePositionCommand;
import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;

public interface PositionUseCase {

    PositionCreateResponse create(String workspaceKey, CreatePositionCommand cmd, Long memberId);

    void update(String workspaceKey, Long positionId, UpdatePositionCommand cmd, Long memberId);

    void delete(String workspaceKey, Long positionId, Long memberId);

    PositionDetail getPosition(String workspaceKey, Long positionId, Long memberId);

    PositionDetailList getWorkspacePositions(String workspaceKey, Long memberId);

    // TODO: Position 검색 (pagination)
}
