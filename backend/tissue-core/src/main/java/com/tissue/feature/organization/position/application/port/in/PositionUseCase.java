package com.tissue.feature.organization.position.application.port.in;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.UpdatePositionCommand;
import com.tissue.feature.organization.position.application.dto.response.GetPositions;
import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;

public interface PositionUseCase {

    PositionCreateResponse create(CreatePositionCommand cmd, WorkspaceMemberContext actorContext);

    void update(Long positionId, UpdatePositionCommand cmd, WorkspaceMemberContext actorContext);

    void delete(Long positionId, WorkspaceMemberContext actorContext);

    PositionDetail getPositionDetail(Long positionId, WorkspaceMemberContext actorContext);

    GetPositions getPositions(WorkspaceMemberContext actorContext);
}
