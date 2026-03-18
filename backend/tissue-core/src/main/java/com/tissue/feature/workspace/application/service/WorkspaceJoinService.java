package com.tissue.feature.workspace.application.service;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.policy.MemberPolicy;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.policy.WorkspacePolicy;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceJoinService {

    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
    private final WorkspacePolicy workspacePolicy;
    private final MemberPolicy memberPolicy;

    public WorkspaceMember join(Workspace workspace, Member member, WorkspaceRole role) {
        Optional<WorkspaceMember> existing = workspaceMemberFinder.getOptionalIncludingSoftDeleted(workspace, member);

        if (existing.isPresent() && !existing.get().isSoftDeleted()) {
            return existing.get();
        }

        checkWorkspaceCapacity(workspace);
        checkMemberJoinCapacity(member);

        return existing.map(returningMember -> {
                    returningMember.restoreSoftDeleted();
                    return returningMember;
                })
                .orElseGet(() -> {
                    WorkspaceMember newMember = WorkspaceMember.create(member, workspace, role);
                    return workspaceMemberCommandRepository.save(newMember);
                });
    }

    private void checkWorkspaceCapacity(Workspace workspace) {
        int currentCount = workspaceMemberFinder.countTotalMembersIncludingSoftDeleted(workspace.getKey());
        workspacePolicy.ensureCanAddMember(currentCount);
    }

    private void checkMemberJoinCapacity(Member member) {
        int joinedCount = workspaceMemberFinder.countJoinedWorkspaces(member);
        memberPolicy.ensureCanJoinWorkspace(joinedCount);
    }
}
