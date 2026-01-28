package com.tissue.project.application.port.in;

public interface ProjectMemberQueryUseCase {

    // TODO: getProjectMemberDetail

    // TODO: ProjectMember pagination api
    //  search by
    //   - name -> workspaceMember.member.name
    //   - username -> workspaceMember.member.username
    //   - display name -> workspaceMember.displayName
    //   - ProjectRole
    //   - 해당 Project에서 활성화된 Issue(initial 또는 terminal state가 아닌 issue)에 참여 중인 ProjectMember들
    //  (optional)
    //   - 해당 Project에 활성화된 Sprint에 참여중인 ProjectMember들 (optional)
    //  sort by
    //   - name alphabet
    //   - displayName alphabet
    //   - ProjectRole(default, 높은순)
    //   - 참여 순
}
