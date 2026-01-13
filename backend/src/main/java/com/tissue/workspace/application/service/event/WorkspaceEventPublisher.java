package com.tissue.workspace.application.service.event;

import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.event.MemberJoinedWorkspaceEvent;
import com.tissue.workspace.domain.event.WorkspaceRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishMemberJoinedWorkspace(WorkspaceMember newMember) {
        eventPublisher.publishEvent(MemberJoinedWorkspaceEvent.create(
                newMember.getWorkspaceKey(),
                newMember.getMemberId(),
                newMember.getEmail(),
                newMember.getDisplayName()));
    }

    public void publishWorkspaceRoleChanged(
            WorkspaceMember targetMember,
            WorkspaceRole oldRole,
            WorkspaceRole newRole,
            Long actorMemberId) {
        eventPublisher.publishEvent(WorkspaceRoleChangedEvent.create(
                targetMember.getWorkspaceKey(),
                targetMember.getMemberId(),
                oldRole,
                newRole,
                actorMemberId));
    }
}
