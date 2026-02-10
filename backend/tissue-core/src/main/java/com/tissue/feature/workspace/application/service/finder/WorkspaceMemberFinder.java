package com.tissue.feature.workspace.application.service.finder;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.application.port.out.WorkspaceMemberQueryRepository;
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

    // TODO: Join fetch with Member
    public WorkspaceMember getBy(String workspaceKey, Long memberId) {
        return workspaceMemberQueryRepository
                .findByWorkspaceKeyAndMember_Id(workspaceKey, memberId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException(workspaceKey, memberId));
    }

    public WorkspaceMember getBy(Workspace workspace, Long memberId) {
        return workspaceMemberQueryRepository
                .findByWorkspaceAndMember_Id(workspace, memberId)
                .orElseThrow(() -> new WorkspaceMemberNotFoundException(workspace.getKey(), memberId));
    }

    public Optional<WorkspaceMember> getOptionalBy(Workspace workspace, Member member) {
        return workspaceMemberQueryRepository.findByWorkspaceAndMember(workspace, member);
    }

    public List<WorkspaceMember> getAllBy(String workspaceKey, Collection<Long> memberIds) {
        return workspaceMemberQueryRepository.findAllByWorkspaceKeyAndMember_IdIn(workspaceKey, memberIds);
    }

    public Set<Long> getJoinedMemberIdsBy(String workspaceKey, Collection<Long> memberIds) {
        return workspaceMemberQueryRepository.findJoinedMemberIds(workspaceKey, memberIds);
    }

    public int countTotalMembersBy(String workspaceKey) {
        return (int) workspaceMemberQueryRepository.countByWorkspaceKey(workspaceKey);
    }

    public int countOwnedWorkspacesBy(Member member) {
        return (int) workspaceMemberQueryRepository.countByMemberAndRole(member, WorkspaceRole.OWNER);
    }

    public int countJoinedWorkspacesBy(Member member) {
        return (int) workspaceMemberQueryRepository.countByMember(member);
    }
}
