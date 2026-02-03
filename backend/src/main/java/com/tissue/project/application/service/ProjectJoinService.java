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
        // TODO: existsBy는 softDelete=true인 경우까지 포함해서 확인함
        //  만약 모종의 이유로 탈퇴했다가 다시 들어가는 경우라면 어떻게 처리?
        if (projectMemberFinder.existsBy(project, workspaceMember.getMemberId())) {
            return;
        }

        ProjectMember projectMember = ProjectMember.create(project, workspaceMember);
        projectMemberRepository.save(projectMember);
    }
}
