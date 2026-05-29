package com.tissue.feature.project.application.service.validator;

import com.tissue.feature.project.application.port.repository.ProjectQueryRepository;
import com.tissue.feature.project.domain.exception.DuplicateProjectKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectValidator {

    private final ProjectQueryRepository projectRepository;

    public void ensureUniqueProjectKey(String projectKey) {
        if (projectRepository.existsByKey(projectKey)) {
            throw new DuplicateProjectKeyException(projectKey);
        }
    }
}
