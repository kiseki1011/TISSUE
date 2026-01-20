package com.tissue.workspace.application.service.event;

import com.tissue.common.enums.JoinMethod;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.workspace.domain.event.WorkspaceRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishMemberJoinedWorkspace(
            WorkspaceMember joinedWorkspaceMember,
            JoinMethod joinMethod,
            Long actorMemberId,
            @Nullable String actorDisplayName) {

        eventPublisher.publishEvent(MemberJoinedWorkspaceEvent.create(
                joinedWorkspaceMember.getWorkspaceKey(),
                joinedWorkspaceMember.getWorkspace().getId(),
                joinedWorkspaceMember.getId(),
                joinedWorkspaceMember.getMemberId(),
                joinedWorkspaceMember.getEmail(),
                joinedWorkspaceMember.getDisplayName(),
                joinedWorkspaceMember.getRole(),
                joinMethod,
                actorMemberId,
                actorDisplayName));
    }

    public void publishWorkspaceRoleChanged(
            WorkspaceMember targetWorkspaceMember,
            WorkspaceRole oldRole,
            WorkspaceRole newRole,
            Long actorMemberId,
            String actorDisplayName) {

        eventPublisher.publishEvent(WorkspaceRoleChangedEvent.create(
                targetWorkspaceMember.getWorkspaceKey(),
                targetWorkspaceMember.getId(),
                targetWorkspaceMember.getMemberId(),
                targetWorkspaceMember.getDisplayName(),
                oldRole,
                newRole,
                actorMemberId,
                actorDisplayName));
    }
}
