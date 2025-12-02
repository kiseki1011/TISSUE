package com.tissue.api.workspace.domain.policy;

import com.tissue.api.workspace.domain.exception.WorkspaceMemberLimitExceededException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkspacePolicy {

	// TODO:도메인 계층에 스프링 기술 의존을 해도 괜찮다면 @Value("${workspace.policy.max-members}") 사용 고려
	private final int maxMembers;

	public void ensureCanAddMember(String workspaceKey, int currentCount) {
		if (currentCount >= maxMembers) {
			throw new WorkspaceMemberLimitExceededException(workspaceKey, maxMembers);
		}
	}

	// TODO: ensureCanAddProject
}
