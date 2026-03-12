package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.response.ProjectMemberResponse;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Set;

public interface ProjectMemberUseCase {

    ProjectMembersResponse addMembers(ProjectIdentifier pid, Set<Long> targetMemberIds, Long actorMemberId);

    ProjectMemberResponse join(ProjectIdentifier pid, Long actorMemberId);

    // TODO: add API endpoint
    void changeRole(ProjectIdentifier pid, Long targetMemberId, ProjectRole role, Long actorMemberId);

    void kickMember(ProjectIdentifier pid, Long targetMemberId, Long actorMemberId);

    void leave(ProjectIdentifier pid, Long actorMemberId);

    // TODO: getProjectMemberDetail

    // TODO: ProjectMember pagination api
    //  search by
    //   - name -> workspaceMember.member.name
    //   - username -> workspaceMember.member.username
    //   - display name -> workspaceMember.displayName
    //   - ProjectRole
    //   - assigned issue's
    //  sort by
    //   - ProjectRole(default, DESC)
    //   - name alphabet
    //   - displayName alphabet
    //   - username alphabet
    //   - joined DESC
}
