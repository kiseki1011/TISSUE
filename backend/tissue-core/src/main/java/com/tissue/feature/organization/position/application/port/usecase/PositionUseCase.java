package com.tissue.feature.organization.position.application.port.usecase;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.UpdatePositionCommand;
import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;

public interface PositionUseCase {

    PositionCreateResponse create(CreatePositionCommand cmd, WorkspaceMemberContext actorContext);

    void update(Long positionId, UpdatePositionCommand cmd, WorkspaceMemberContext actorContext);

    void delete(Long positionId, WorkspaceMemberContext actorContext);

    PositionDetail getPosition(Long positionId, WorkspaceMemberContext actorContext);

    PositionDetailList getWorkspacePositions(WorkspaceMemberContext actorContext);

    // TODO: Position 검색 (pagination)
}
