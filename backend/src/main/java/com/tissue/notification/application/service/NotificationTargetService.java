package com.tissue.notification.application.service;

import com.tissue.workspace.application.port.out.WorkspaceMemberContact;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationTargetService {

    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    /** Retrieve all members in the workspace as notification targets. */
    public List<WorkspaceMemberContact> getWorkspaceWideMemberTargets(String workspaceCode) {
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKey(workspaceCode);
    }

    /** Retrieve all members in the workspace as notification targets, excluding a specific member. */
    public List<WorkspaceMemberContact> getAllWorkspaceMembersExcluding(String workspaceCode, Long excludedMemberId) {
        return workspaceMemberQueryRepository.findAllContactsByWorkspaceKeyExcluding(workspaceCode, excludedMemberId);
    }

    /** Retrieve workspace administrators and a specific member as notification targets. */
    public Set<WorkspaceMemberContact> getAdminAndSpecificMemberTargets(String workspaceCode, Long memberId) {

        Set<WorkspaceMemberContact> targets = workspaceMemberQueryRepository.findAdminContactsByWorkspace_Key(
                workspaceCode, Set.of(WorkspaceRole.ADMIN, WorkspaceRole.OWNER));

        workspaceMemberQueryRepository
                .findContactByMemberIdAndWorkspaceKey(memberId, workspaceCode)
                .ifPresent(targets::add);

        return targets;
    }

    /** Retrieve a specific member as a notification target. */
    public Set<WorkspaceMemberContact> getSpecificMemberTarget(String workspaceCode, Long memberId) {

        Set<WorkspaceMemberContact> target = new HashSet<>();

        workspaceMemberQueryRepository
                .findContactByMemberIdAndWorkspaceKey(memberId, workspaceCode)
                .ifPresent(target::add);

        return target;
    }
}
