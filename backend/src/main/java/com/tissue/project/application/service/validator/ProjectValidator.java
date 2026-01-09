package com.tissue.project.application.service.validator;

import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.DuplicateProjectKeyException;
import com.tissue.project.domain.exception.ProjectMemberAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectValidator {

    private final ProjectQueryRepository projectRepository;
    private final ProjectMemberQueryRepository projectMemberRepository;

    public void ensureUniqueProjectKey(String projectKey, String workspaceKey) {
        if (projectRepository.existsByKeyAndWorkspaceKey(projectKey, workspaceKey)) {
            throw new DuplicateProjectKeyException(workspaceKey, projectKey);
        }
    }

    public void ensureNotAlreadyJoined(Project project, Long memberId) {
        if (projectMemberRepository.existsByProjectAndMemberId(project, memberId)) {
            throw new ProjectMemberAlreadyExistsException(project.getWorkspaceKey(), project.getKey(), memberId);
        }
    }
}
