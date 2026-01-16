package com.tissue.project.application.service.event;

import com.tissue.common.enums.JoinMethod;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.event.MemberJoinedProjectEvent;
import com.tissue.project.domain.event.ProjectRoleChangedEvent;
import com.tissue.workspace.domain.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishMemberJoinedProject(
            ProjectMember newMember,
            Workspace workspace,
            Project project,
            JoinMethod joinMethod,
            Long actorMemberId,
            String actorDisplayName) {
        eventPublisher.publishEvent(MemberJoinedProjectEvent.create(
                workspace.getKey(),
                workspace.getId(),
                project.getKey(),
                project.getId(),
                newMember.getMemberId(),
                newMember.getWorkspaceMember().getEmail(),
                newMember.getDisplayName(),
                newMember.getRole(),
                joinMethod,
                actorMemberId,
                actorDisplayName));
    }

    public void publishProjectRoleChanged(
            ProjectMember targetMember,
            ProjectRole oldRole,
            ProjectRole newRole,
            Long actorMemberId,
            String actorDisplayName) {

        eventPublisher.publishEvent(ProjectRoleChangedEvent.create(
                targetMember.getWorkspaceKey(),
                targetMember.getProjectKey(),
                targetMember.getMemberId(),
                oldRole,
                newRole,
                actorMemberId,
                actorDisplayName,
                targetMember.getDisplayName()));
    }
}
