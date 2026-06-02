package com.tissue.feature.notification.application.port.usecase;

import com.tissue.feature.project.domain.event.ProjectHardDeletedEvent;
import com.tissue.feature.project.domain.event.ProjectRoleChangedEvent;

public interface ProjectNotificationUseCase {

    void handleProjectRoleChanged(ProjectRoleChangedEvent event);

    void handleProjectHardDeleted(ProjectHardDeletedEvent event);
}
