package com.tissue.feature.project.application.service;

import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.event.ProjectHardDeletedEvent;
import com.tissue.feature.project.domain.event.ProjectRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishRoleChanged(ProjectMember target, ProjectRole oldRole, ProjectMember actor) {
        eventPublisher.publishEvent(ProjectRoleChangedEvent.create(
                target.getProjectKey(),
                target.getMemberId(),
                target.getDisplayName(),
                oldRole,
                target.getRole(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishHardDeleted(String projectKey) {
        eventPublisher.publishEvent(ProjectHardDeletedEvent.create(projectKey));
    }
}
