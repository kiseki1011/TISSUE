package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.feature.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Set;

public interface ProjectMemberUseCase {

    ProjectMembersCommandResult addMembers(
            ProjectIdentifier projectIdentifier, Set<Long> targetMemberIds, Long memberId);

    // TODO: Add a javadoc that its callable if the project visiblity is PUBLIC
    ProjectMemberCommandResult join(ProjectIdentifier projectIdentifier, Long memberId);

    void changeRole(ProjectIdentifier projectIdentifier, Long targetMemberId, ProjectRole role, Long memberId);

    void kickMember(ProjectIdentifier projectIdentifier, Long targetMemberId, Long memberId);

    void leave(ProjectIdentifier projectIdentifier, Long memberId);

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
