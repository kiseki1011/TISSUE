package com.tissue.feature.sprint.application.service;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.application.port.repository.SprintQueryRepository;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.feature.sprint.domain.exception.SprintNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintFinder {

    private final SprintQueryRepository sprintQueryRepository;

    public Sprint getBy(Long sprintId, Project project) {
        return sprintQueryRepository
                .findByProjectAndId(project, sprintId)
                .orElseThrow(() -> new SprintNotFoundException(project.getKey(), sprintId));
    }

    public Sprint getWithProjectBy(String workspaceKey, String projectKey, Long sprintId) {
        return sprintQueryRepository
                .findWithProjectByWorkspaceKeyAndProjectKeyAndId(workspaceKey, projectKey, sprintId)
                .orElseThrow(() -> new SprintNotFoundException(projectKey, sprintId));
    }

    public Optional<Sprint> getActiveOptional(Project project) {
        return sprintQueryRepository.findByProjectAndStatus(project, SprintStatus.ACTIVE);
    }
}
