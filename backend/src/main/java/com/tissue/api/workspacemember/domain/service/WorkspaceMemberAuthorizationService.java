package com.tissue.api.workspacemember.domain.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberAuthorizationService {

	private final WorkspaceMemberReader workspaceMemberReader;

	public void validateCanModifyTeamPosition(
		Long loginMemberId,
		Long targetMemberId,
		String workspaceCode
	) {
		if (isSelf(loginMemberId, targetMemberId)) {
			return;
		}

		if (hasInsufficientPermission(loginMemberId, workspaceCode)) {
			throw new ForbiddenOperationException("MANAGER 이상의 권한이 필요합니다.");
		}
	}

	private static boolean isSelf(Long loginMemberId, Long targetMemberId) {
		return Objects.equals(loginMemberId, targetMemberId);
	}

	private boolean hasInsufficientPermission(Long memberId, String workspaceCode) {
		return workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode)
			.roleIsLowerThan(WorkspaceRole.MANAGER);
	}
}
