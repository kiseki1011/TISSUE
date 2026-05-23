package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface WorkspaceMemberQueryUseCase {

    List<WorkspaceMemberSearchResponse> searchMembers(
            String workspaceKey, @Nullable String projectKey, String query, Long actorMemberId);

    // TODO: getWorkspaceMemberDetail
    //   - name
    //   - username
    //   - WorkspaceRole
    //   - email (고민중)
    //   - 참여 date time
    //   - 참여 중인 project들(projectKey-projectRole)
}
