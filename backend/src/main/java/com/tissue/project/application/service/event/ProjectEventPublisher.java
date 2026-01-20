package com.tissue.project.application.service.event;

import com.tissue.common.enums.JoinMethod;
import com.tissue.project.application.dto.ProjectMemberContext;
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
            JoinMethod joinMethod,
            Long actorMemberId,
            String actorDisplayName) {

        eventPublisher.publishEvent(MemberJoinedProjectEvent.create(
                workspace.getKey(),
                workspace.getId(),
                newMember.getProjectKey(),
                newMember.getProject().getId(),
                newMember.getId(),
                newMember.getMemberId(),
                newMember.getWorkspaceMember().getDisplayName(),
                newMember.getRole(),
                joinMethod,
                actorMemberId,
                actorDisplayName));
    }

    public void publishProjectRoleChanged(
            ProjectMember targetProjectMember,
            ProjectRole oldRole,
            ProjectRole newRole,
            ProjectMemberContext actorContext) {

        eventPublisher.publishEvent(ProjectRoleChangedEvent.create(
                actorContext.workspaceKey(),
                actorContext.workspaceId(),
                actorContext.projectKey(),
                actorContext.projectId(),
                targetProjectMember.getId(),
                targetProjectMember.getMemberId(),
                targetProjectMember.getWorkspaceMember().getDisplayName(),
                oldRole,
                newRole,
                actorContext.memberId(),
                actorContext.displayName()));
    }
}
