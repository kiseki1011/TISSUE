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
            ProjectMember joinedMember,
            Workspace workspace,
            Project project,
            JoinMethod joinMethod,
            Long actorMemberId) {

        eventPublisher.publishEvent(MemberJoinedProjectEvent.create(
                joinedMember.getWorkspaceKey(),
                workspace.getId(),
                project.getKey(),
                project.getId(),
                joinedMember.getMemberId(),
                joinedMember.getWorkspaceMember().getEmail(),
                joinedMember.getDisplayName(),
                joinedMember.getRole(),
                joinMethod,
                actorMemberId));
    }

    public void publishProjectRoleChanged(
            ProjectMember targetMember, ProjectRole oldRole, ProjectRole newRole, Long actorMemberId) {

        eventPublisher.publishEvent(ProjectRoleChangedEvent.create(
                targetMember.getWorkspaceKey(),
                targetMember.getProjectKey(),
                targetMember.getMemberId(),
                oldRole,
                newRole,
                actorMemberId));
    }
}
