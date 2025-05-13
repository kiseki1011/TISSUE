package com.tissue.api.workspacemember.domain.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberPermissionValidator {

	public void validateCanRemoveWorkspaceMember(
		WorkspaceMember requester,
		WorkspaceMember target
	) {
		if (isSelf(requester, target)) {
			throw new InvalidOperationException(
				"Cannot remove yourself. Use DELETE api/v1/workspaces/" + requester.getWorkspaceCode() + "/members"
			);
		}

		validateRequesterHasHigherRole(requester, target);
	}

	public void validateCanUpdateRole(WorkspaceMember requester, WorkspaceMember target) {
		if (isSelf(requester, target)) {
			throw new InvalidOperationException("Cannot update your own role");
		}

		validateRequesterHasHigherRole(requester, target);
	}

	public void validateCanModifyTeamPosition(WorkspaceMember requester, WorkspaceMember target) {
		if (isSelf(requester, target)) {
			return;
		}

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			throw new ForbiddenOperationException("MANAGER 이상의 권한이 필요합니다.");
		}
	}

	private void validateRequesterHasHigherRole(WorkspaceMember requester, WorkspaceMember target) {
		if (target.getRole().getLevel() >= requester.getRole().getLevel()) {
			throw new ForbiddenOperationException(
				String.format(
					"You must have a higher role than the target. your role: %s, target's role: %s",
					requester.getRole(), target.getRole()
				)
			);
		}
	}

	private boolean isSelf(WorkspaceMember requester, WorkspaceMember target) {
		return Objects.equals(requester.getId(), target.getId());
	}
}
