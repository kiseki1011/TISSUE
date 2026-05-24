package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.response.query.DeletedWorkspaceSummary;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import java.util.List;

public interface WorkspaceQueryUseCase {

    WorkspaceDetail getDetail(String workspaceKey, Long actorMemberId);

    List<WorkspaceSummaryResponse> getMyWorkspaces(Long actorMemberId);

    List<DeletedWorkspaceSummary> getMyDeletedWorkspaces(Long actorMemberId);

    void checkKeyAvailability(String key);
}
