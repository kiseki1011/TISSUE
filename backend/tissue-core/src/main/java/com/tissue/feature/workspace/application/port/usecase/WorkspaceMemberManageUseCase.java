package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface WorkspaceMemberManageUseCase {

    void updateDisplayName(String workspaceKey, Long targetMemberId, String displayName, Long actorMemberId);

    void updateRole(String workspaceKey, Long targetMemberId, WorkspaceRole grantRole, Long actorMemberId);

    void addPosition(String workspaceKey, Long targetMemberId, Long positionId, Long actorMemberId);

    void removePosition(String workspaceKey, Long targetMemberId, Long positionId, Long actorMemberId);

    void addTeam(String workspaceKey, Long targetMemberId, Long teamId, Long actorMemberId);

    void removeTeam(String workspaceKey, Long targetMemberId, Long teamId, Long actorMemberId);

    List<WorkspaceMemberSearchResponse> searchMembers(
            String workspaceKey, @Nullable String projectKey, String query, Long actorMemberId);

    // TODO: WorkspaceMember pagination api
    //  search by
    //   - name
    //   - username
    //   - display name
    //   - WorkspaceRole
    //  sort by
    //   - name alphabet
    //   - WorkspaceRole(default, 높은순)
    //   - 참여 순
    //  each item schema
    //   - name
    //   - username
    //   - display name
    //   - WorkspaceRole
    //   - 참여 중인 project들(projectKey-projectRole)

    // TODO: getWorkspaceMemberDetail
    //   - name
    //   - username
    //   - display name
    //   - WorkspaceRole
    //   - email (고민중)
    //   - 참여 date time
    //   - 참여 중인 project들(projectKey-projectRole)
}
