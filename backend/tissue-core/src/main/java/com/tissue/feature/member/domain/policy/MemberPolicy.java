package com.tissue.feature.member.domain.policy;

import static com.tissue.feature.member.domain.exception.MemberErrorCode.WORKSPACE_JOIN_LIMIT_EXCEEDED;
import static com.tissue.feature.member.domain.exception.MemberErrorCode.WORKSPACE_OWNAGE_LIMIT_EXCEEDED;

import com.tissue.shared.exception.base.BadRequestException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberPolicy {

    private final int maxOwnedWorkspaces;
    private final int maxJoinedWorkspaces;

    public void ensureCanCreateWorkspace(int currentOwnedCount, int currentJoinedCount) {
        if (currentOwnedCount >= maxOwnedWorkspaces) {
            throw new BadRequestException(WORKSPACE_OWNAGE_LIMIT_EXCEEDED)
                    .addContext("workspaceCreateLimit", maxOwnedWorkspaces);
        }
        ensureCanJoinWorkspace(currentJoinedCount);
    }

    public void ensureCanJoinWorkspace(int currentJoinedCount) {
        if (currentJoinedCount >= maxJoinedWorkspaces) {
            throw new BadRequestException(WORKSPACE_JOIN_LIMIT_EXCEEDED)
                    .addContext("workspaceJoinLimit", maxJoinedWorkspaces);
        }
    }
}
