package com.tissue.feature.project.application.listener;

import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.workspace.domain.event.WorkspaceDeletedEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectCleanupEventListener {

    private final ProjectCommandRepository projectCommandRepository;

    @EventListener
    public void onWorkspaceDeleted(WorkspaceDeletedEvent event) {
        log.info(
                "Cascading soft-delete for all projects in workspace: {} triggered by workspace deletion.",
                event.workspaceKey());
        projectCommandRepository.softDeleteAllByWorkspaceKey(event.workspaceKey(), Instant.now());
    }
}
