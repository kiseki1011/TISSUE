package com.tissue.workspace.application.service.finder;

import com.tissue.member.domain.Member;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;
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

    // TODO: find -> get
    public Optional<WorkspaceMember> findAnyOptionalBy(Long memberId, String workspaceKey) {
        return workspaceMemberQueryRepository.findAnyByMemberIdAndWorkspaceKey(memberId, workspaceKey);
    }

    // TODO: find -> get
    public WorkspaceMember findBy(Long memberId, String workspaceKey) {
        return workspaceMemberQueryRepository
                .findByMember_IdAndWorkspaceKey(memberId, workspaceKey)
                .orElseThrow(() -> WorkspaceExceptions.memberNotFound(memberId, workspaceKey));
    }

    // TODO: find -> get
    public WorkspaceMember findBy(Long memberId, Workspace workspace) {
        return workspaceMemberQueryRepository
                .findByMember_IdAndWorkspace(memberId, workspace)
                .orElseThrow(() -> WorkspaceExceptions.memberNotFound(memberId, workspace.getKey()));
    }

    // TODO: find -> get
    public Optional<WorkspaceMember> findOptionalBy(Member member, Workspace workspace) {
        return workspaceMemberQueryRepository.findByMemberAndWorkspace(member, workspace);
    }

    public boolean existsBy(Member member, Workspace workspace) {
        return workspaceMemberQueryRepository.existsByMemberAndWorkspace(member, workspace);
    }

    // TODO: find -> get
    public List<WorkspaceMember> findAllBy(Collection<Long> memberIds, String workspaceKey) {
        return workspaceMemberQueryRepository.findAllByMember_IdInAndWorkspaceKey(memberIds, workspaceKey);
    }

    // TODO: find -> get
    public Set<Long> findJoinedMemberIdsBy(String workspaceKey, Collection<Long> memberIds) {
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
