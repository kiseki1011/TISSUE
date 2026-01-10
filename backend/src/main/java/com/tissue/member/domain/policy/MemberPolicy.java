package com.tissue.member.domain.policy;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.WorkspaceJoinLimitExceededException;
import com.tissue.member.domain.exception.WorkspaceOwnageLimitExceededException;

public class MemberPolicy {

    private final int maxOwnedWorkspaces;
    private final int maxJoinedWorkspaces;

    public MemberPolicy(int maxOwnedWorkspaces, int maxJoinedWorkspaces) {
        this.maxOwnedWorkspaces = maxOwnedWorkspaces;
        this.maxJoinedWorkspaces = maxJoinedWorkspaces;
    }

    public void ensureCanCreateWorkspace(int currentOwnedCount, int currentJoinedCount, Member member) {
        if (currentOwnedCount >= maxOwnedWorkspaces) {
            throw new WorkspaceOwnageLimitExceededException(member, maxOwnedWorkspaces);
        }

        ensureCanJoinWorkspace(currentJoinedCount, member);
    }

    public void ensureCanJoinWorkspace(int currentJoinedCount, Member member) {
        if (currentJoinedCount >= maxJoinedWorkspaces) {
            throw new WorkspaceJoinLimitExceededException(member, maxJoinedWorkspaces);
        }
    }
}
