package com.tissue.project.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import java.util.Set;

public interface ProjectMemberUseCase {

    ProjectMembersCommandResult addMembers(Set<Long> targetMemberIds, ProjectMemberContext actorContext);

    // TODO: Add a javadoc that its callable if the project visiblity is PUBLIC
    ProjectMemberCommandResult join(String projectKey, WorkspaceMemberContext actorContext);

    void kickMember(Long targetMemberId, ProjectMemberContext actorContext);

    void leave(ProjectMemberContext actorContext);
}
