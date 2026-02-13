package com.tissue.feature.workspace.application.service.finder;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceMemberNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// TODO: IncludingSoftDeleted 가 필요한 케이스인지 따지고 대체
@Component
@RequiredArgsConstructor
public class WorkspaceMemberFinder {

    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    public WorkspaceMember getActiveWithWorkspace(String workspaceKey, Long memberId) {
        return workspaceMemberQueryRepository
                .findActiveWithWorkspaceByWorkspaceKeyAndMemberId(workspaceKey, memberId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException(workspaceKey, memberId));
    }

    public Optional<WorkspaceMember> getOptionalIncludingSoftDeleted(Workspace workspace, Member member) {
        return workspaceMemberQueryRepository.findByWorkspaceAndMember(workspace, member);
    }

    public List<WorkspaceMember> getAllIncludingSoftDeleted(String workspaceKey, Collection<Long> memberIds) {
        return workspaceMemberQueryRepository.findAllByWorkspaceKeyAndMember_IdIn(workspaceKey, memberIds);
    }

    public Set<Long> getJoinedMemberIdsBy(String workspaceKey, Collection<Long> memberIds) {
        return workspaceMemberQueryRepository.findJoinedMemberIds(workspaceKey, memberIds);
    }

    public int countTotalMembersIncludingSoftDeleted(String workspaceKey) {
        return (int) workspaceMemberQueryRepository.countByWorkspaceKey(workspaceKey);
    }

    public int countOwnedWorkspacesBy(Member member) {
        return (int) workspaceMemberQueryRepository.countByMemberAndRole(member, WorkspaceRole.OWNER);
    }

    public int countJoinedWorkspacesBy(Member member) {
        return (int) workspaceMemberQueryRepository.countByMember(member);
    }
}
