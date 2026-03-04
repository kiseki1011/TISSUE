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

@Component
@RequiredArgsConstructor
public class WorkspaceMemberFinder {

    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    public WorkspaceMember getWithWorkspace(String workspaceKey, Long memberId) {
        return workspaceMemberQueryRepository
                .findWithWorkspaceByWorkspaceKeyAndMemberId(workspaceKey, memberId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException(workspaceKey, memberId));
    }

    public Optional<WorkspaceMember> getOptionalIncludingSoftDeleted(Workspace workspace, Member member) {
        return workspaceMemberQueryRepository.findByWorkspaceAndMemberIncludingSoftDeleted(workspace, member);
    }

    public List<WorkspaceMember> getAllIncludingSoftDeleted(String workspaceKey, Collection<Long> memberIds) {
        return workspaceMemberQueryRepository.findAllByWorkspaceKeyAndMemberIdsIncludingSoftDeleted(
                workspaceKey, memberIds);
    }

    public Set<Long> getJoinedMemberIds(String workspaceKey, Collection<Long> memberIds) {
        return workspaceMemberQueryRepository.findJoinedMemberIds(workspaceKey, memberIds);
    }

    public int countTotalMembersIncludingSoftDeleted(String workspaceKey) {
        return (int) workspaceMemberQueryRepository.countByWorkspaceKeyIncludingSoftDeleted(workspaceKey);
    }

    public int countOwnedWorkspaces(Member member) {
        return (int) workspaceMemberQueryRepository.countByMemberAndRole(member, WorkspaceRole.OWNER);
    }

    public int countJoinedWorkspaces(Member member) {
        return (int) workspaceMemberQueryRepository.countByMember(member);
    }
}
