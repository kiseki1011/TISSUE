package com.tissue.member.domain.policy;

import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.MemberExceptions;

public class MemberPolicy {

	private final int maxOwnedWorkspaces;
	private final int maxJoinedWorkspaces;

	public MemberPolicy(int maxOwnedWorkspaces, int maxJoinedWorkspaces) {
		this.maxOwnedWorkspaces = maxOwnedWorkspaces;
		this.maxJoinedWorkspaces = maxJoinedWorkspaces;
	}

	public void ensureCanCreateWorkspace(int currentOwnedCount, int currentJoinedCount, Member member) {
		if (currentOwnedCount >= maxOwnedWorkspaces) {
			throw MemberExceptions.workspaceOwnageLimit(member, maxOwnedWorkspaces);
		}

		ensureCanJoinWorkspace(currentJoinedCount, member);
	}

	public void ensureCanJoinWorkspace(int currentJoinedCount, Member member) {
		if (currentJoinedCount >= maxJoinedWorkspaces) {
			throw MemberExceptions.workspaceJoinLimit(member, maxJoinedWorkspaces);
		}
	}
}
