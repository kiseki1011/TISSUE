package com.tissue.api.member.domain.policy;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.exception.WorkspaceLimitExceededException;

public class MemberPolicy {

	private final int maxOwnedWorkspaces;
	private final int maxJoinedWorkspaces;

	public MemberPolicy(int maxOwnedWorkspaces, int maxJoinedWorkspaces) {
		this.maxOwnedWorkspaces = maxOwnedWorkspaces;
		this.maxJoinedWorkspaces = maxJoinedWorkspaces;
	}

	public void ensureCanCreateWorkspace(int currentOwnedCount, int currentJoinedCount, Member member) {
		if (currentOwnedCount >= maxOwnedWorkspaces) {
			throw new WorkspaceLimitExceededException(
				"Member(id: '%d') cannot create more than %d workspaces."
					.formatted(member.getId(), maxOwnedWorkspaces),
				member.getId()
			);
		}

		ensureCanJoinWorkspace(currentJoinedCount, member);
	}

	public void ensureCanJoinWorkspace(int currentJoinedCount, Member member) {
		if (currentJoinedCount >= maxJoinedWorkspaces) {
			throw new WorkspaceLimitExceededException(
				"Member(id: '%d') cannot join more than %d workspaces."
					.formatted(member.getId(), maxJoinedWorkspaces),
				member.getId()
			);
		}
	}
}
