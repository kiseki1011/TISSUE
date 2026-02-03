package com.tissue.project.application.service.validator;

import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.exception.DuplicateProjectKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectValidator {

    private final ProjectQueryRepository projectRepository;

    public void ensureUniqueProjectKey(String projectKey, String workspaceKey) {
        if (projectRepository.existsByKeyAndWorkspaceKey(projectKey, workspaceKey)) {
            throw new DuplicateProjectKeyException(workspaceKey, projectKey);
        }
    }
}
