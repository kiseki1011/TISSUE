package com.tissue.api.workspacemember.domain.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberPermissionValidator {

	public void validateCanRemoveWorkspaceMember(WorkspaceMember requester, WorkspaceMember target) {
		if (Objects.equals(requester, target)) {
			// TODO: SelfModificationNotAllowedException extends BadRequestException
			//  - 더 좋은 이름이 있을까?
			throw new RuntimeException("Cannot remove yourself."); // workspaceKey, requester.getMemberId()
		}

		ensureTargetHasLowerRole(requester, target);
	}

	public void validateCanUpdateRole(WorkspaceMember requester, WorkspaceMember target) {
		if (Objects.equals(requester, target)) {
			// TODO: SelfModificationNotAllowedException
			throw new RuntimeException("Cannot update your own role"); // workspaceKey, requester.getMemberId()
		}

		ensureTargetHasLowerRole(requester, target);
	}

	private void ensureTargetHasLowerRole(WorkspaceMember requester, WorkspaceMember target) {
		if (target.roleIsLowerThan(requester.getRole())) {
			// TODO: HigherWorkspaceRoleRequiredException extends ForbiddenException
			//  - 더 좋은 이름이 있을까?
			throw new RuntimeException(
				String.format(
					"Requester must have a higher role than the target. requester role: %s, target's role: %s",
					requester.getRole(), target.getRole()
				)
				// TODO: 그런데 workspaceKey + memberId 말고 그냥 WorkspaceMember의 id를 넘기는건 별로이겠지?
				// workspaceKey, requester.getMemberId(), requester.getRole(), target.getMemberId(), target.getRole()
			);
		}
	}
}
