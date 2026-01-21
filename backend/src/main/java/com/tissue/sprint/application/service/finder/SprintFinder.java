package com.tissue.sprint.application.service.finder;

import com.tissue.project.domain.Project;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.enums.SprintStatus;
import com.tissue.sprint.domain.exception.SprintNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintFinder {

    private final SprintQueryRepository sprintQueryRepository;

    public Sprint getBy(Long sprintId, Project project) {
        return sprintQueryRepository
                .findByIdAndProject(sprintId, project)
                .orElseThrow(() -> new SprintNotFoundException(sprintId, project));
    }

    public Optional<Sprint> getActiveOptional(Project project) {
        return sprintQueryRepository.findByProjectAndStatus(project, SprintStatus.ACTIVE);
    }

    public boolean existsActiveSprintByProject(Project project) {
        return sprintQueryRepository.existsByProjectAndStatus(project, SprintStatus.ACTIVE);
    }
}
