package com.tissue.project.application.service;

import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.workspace.domain.WorkspaceMember;
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
