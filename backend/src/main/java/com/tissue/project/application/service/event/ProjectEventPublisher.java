package com.tissue.project.application.service.event;

import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.event.MemberJoinedProjectEvent;
import com.tissue.project.domain.event.ProjectRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishMemberJoinedProject(ProjectMember newMember, Long actorMemberId) {
        eventPublisher.publishEvent(MemberJoinedProjectEvent.create(
                newMember.getWorkspaceKey(),
                newMember.getProjectKey(),
                newMember.getMemberId(),
                newMember.getWorkspaceMember().getEmail(),
                newMember.getDisplayName(),
                actorMemberId));
    }

    public void publishProjectRoleChanged(
            ProjectMember targetMember,
            ProjectRole oldRole,
            ProjectRole newRole,
            Long actorMemberId) {
        eventPublisher.publishEvent(ProjectRoleChangedEvent.create(
                targetMember.getWorkspaceKey(),
                targetMember.getProjectKey(),
                targetMember.getMemberId(),
                oldRole,
                newRole,
                actorMemberId));
    }
}
