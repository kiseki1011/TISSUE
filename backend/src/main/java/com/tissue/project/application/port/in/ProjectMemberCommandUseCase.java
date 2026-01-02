package com.tissue.project.application.port.in;

import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;

public interface ProjectMemberCommandUseCase {

    // TODO: addMembers, kickMember, join을 ProjectParticipationUseCase로 분리할까?
    ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd);

    ProjectMemberCommandResult kickMember(KickProjectMemberCommand cmd);

    ProjectMemberCommandResult joinViaDirect(DirectJoinProjectCommand cmd);

    ProjectMemberCommandResult changeProjectRole(ChangeProjectRoleCommand cmd);

    ProjectMemberCommandResult leave(String workspaceKey, String projectKey, Long meberId);
}
