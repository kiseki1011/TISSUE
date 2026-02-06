package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface WorkspaceMemberManageUseCase {

    void updateDisplayName(Long targetMemberId, String displayName, WorkspaceMemberContext actorContext);

    void updateRole(Long targetMemberId, WorkspaceRole grantRole, WorkspaceMemberContext actorContext);

    void addPosition(Long targetMemberId, Long positionId, WorkspaceMemberContext actorContext);

    void removePosition(Long targetMemberId, Long positionId, WorkspaceMemberContext actorContext);

    void addTeam(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext);

    void removeTeam(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext);

    List<WorkspaceMemberSearchResponse> searchMembers(
            WorkspaceMemberContext context, String query, @Nullable String projectKey);

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
