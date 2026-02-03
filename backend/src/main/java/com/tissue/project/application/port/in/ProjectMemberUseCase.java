package com.tissue.project.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;

public interface ProjectMemberUseCase {

    ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd);

    // TODO: Add a javadoc that its callable if the project visiblity is PUBLIC
    ProjectMemberCommandResult join(DirectJoinProjectCommand cmd);

    void kickMember(KickProjectMemberCommand cmd);

    void leave(ProjectMemberContext actorContext);
}
