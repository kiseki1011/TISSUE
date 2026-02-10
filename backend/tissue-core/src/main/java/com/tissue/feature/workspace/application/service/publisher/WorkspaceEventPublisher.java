package com.tissue.feature.workspace.application.service.publisher;

import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.feature.workspace.domain.event.WorkspaceRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishMemberJoinedWorkspace(
            WorkspaceMember joinedWorkspaceMember, Long actorMemberId, @Nullable String actorDisplayName) {

        eventPublisher.publishEvent(MemberJoinedWorkspaceEvent.create(
                joinedWorkspaceMember.getWorkspaceKey(),
                joinedWorkspaceMember.getWorkspace().getId(),
                joinedWorkspaceMember.getId(),
                joinedWorkspaceMember.getMemberId(),
                joinedWorkspaceMember.getMember().getEmail(),
                joinedWorkspaceMember.getDisplayName(),
                joinedWorkspaceMember.getRole(),
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
