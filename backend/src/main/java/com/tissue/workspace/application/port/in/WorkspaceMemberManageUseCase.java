package com.tissue.workspace.application.port.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.domain.enums.WorkspaceRole;

public interface WorkspaceMemberManageUseCase {

    void updateDisplayName(Long targetMemberId, String displayName, WorkspaceMemberContext actorContext);

    void updateRole(Long targetMemberId, WorkspaceRole grantRole, WorkspaceMemberContext actorContext);

    void addPosition(Long targetMemberId, Long positionId, WorkspaceMemberContext actorContext);

    void removePosition(Long targetMemberId, Long positionId, WorkspaceMemberContext actorContext);

    void addTeam(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext);

    void removeTeam(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext);
}
