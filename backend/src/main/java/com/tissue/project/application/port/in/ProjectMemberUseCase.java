package com.tissue.project.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;

public interface ProjectMemberUseCase {

    ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd);

    // TODO: usecase에 대한 자세한 설명이 더 필요할듯
    //  - 프로젝트 visibility가 public인 경우 초대나 링크 없이 바로 참여가 가능한 버튼 활성화
    ProjectMemberCommandResult joinViaDirect(DirectJoinProjectCommand cmd);

    void kickMember(KickProjectMemberCommand cmd);

    void leave(ProjectMemberContext actorContext);

    void changeProjectRole(ChangeProjectRoleCommand cmd);
}
