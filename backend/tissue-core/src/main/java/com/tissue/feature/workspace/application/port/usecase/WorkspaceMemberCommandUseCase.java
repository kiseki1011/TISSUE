package com.tissue.feature.workspace.application.port.usecase;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;

public interface WorkspaceMemberCommandUseCase {

    void updateRole(String workspaceKey, Long targetMemberId, WorkspaceRole grantRole, Long actorMemberId);

    void addPosition(String workspaceKey, Long targetMemberId, Long positionId, Long actorMemberId);

    void removePosition(String workspaceKey, Long targetMemberId, Long positionId, Long actorMemberId);

    void addTeam(String workspaceKey, Long targetMemberId, Long teamId, Long actorMemberId);

    void removeTeam(String workspaceKey, Long targetMemberId, Long teamId, Long actorMemberId);
}
