package com.tissue.feature.project.application.service;

import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectJoinService {

    private final ProjectMemberFinder projectMemberFinder;
    private final ProjectMemberCommandRepository projectMemberRepository;

    public void join(Project project, WorkspaceMember workspaceMember) {
        if (projectMemberFinder.existsBy(project, workspaceMember.getMemberId())) {
            return;
        }

        ProjectMember projectMember = ProjectMember.create(project, workspaceMember);
        projectMemberRepository.save(projectMember);
    }
}
