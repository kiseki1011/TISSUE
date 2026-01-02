package com.tissue.position.application.port.in;

import com.tissue.position.application.dto.request.CreatePositionCommand;
import com.tissue.position.application.dto.request.UpdatePositionCommand;
import com.tissue.position.application.dto.response.GetPositions;
import com.tissue.position.application.dto.response.PositionCreateResponse;
import com.tissue.position.application.dto.response.PositionDetail;

public interface PositionUseCase {

    PositionCreateResponse create(CreatePositionCommand cmd);

    void update(UpdatePositionCommand cmd);

    void delete(String workspaceKey, Long positionId);

    PositionDetail getPositionDetail(String workspaceKey, Long positionId);

    GetPositions getPositions(String workspaceKey);
}
