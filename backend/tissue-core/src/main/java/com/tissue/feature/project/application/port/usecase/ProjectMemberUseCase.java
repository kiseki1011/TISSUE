package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.response.ProjectMemberResponse;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Set;

public interface ProjectMemberUseCase {

    ProjectMembersResponse addMembers(ProjectIdentifier pid, Set<Long> targetMemberIds, Long actorMemberId);

    ProjectMemberResponse join(ProjectIdentifier pid, Long actorMemberId);

    void changeRole(ProjectIdentifier pid, Long targetMemberId, ProjectRole role, Long actorMemberId);

    void kickMember(ProjectIdentifier pid, Long targetMemberId, Long actorMemberId);

    void leave(ProjectIdentifier pid, Long actorMemberId);

    // ProjectMember list query is owned by ProjectMemberQueryUseCase.
    // Single-member detail endpoint is not yet exposed; add when a real TUI screen
    // needs more than what the list summary already carries.
}
