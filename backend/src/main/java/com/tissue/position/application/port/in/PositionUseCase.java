package com.tissue.position.application.port.in;

import com.tissue.position.application.dto.request.CreatePositionCommand;
import com.tissue.position.application.dto.request.UpdatePositionCommand;
import com.tissue.position.application.dto.response.GetPositions;
import com.tissue.position.application.dto.response.PositionCreateResponse;
import com.tissue.position.application.dto.response.PositionDetail;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public interface PositionUseCase {

    PositionCreateResponse create(CreatePositionCommand cmd);

    void update(UpdatePositionCommand cmd);

    void delete(Long positionId, WorkspaceMemberContext actorContext);

    PositionDetail getPositionDetail(Long positionId, WorkspaceMemberContext actorContext);

    GetPositions getPositions(WorkspaceMemberContext actorContext);
}
