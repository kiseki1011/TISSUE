package com.tissue.api.workspacemember.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.exception.InvalidRoleUpdateException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberValidator {

	public void validateRoleUpdate(WorkspaceMember requester, WorkspaceMember target) {
		validateNotSelfUpdate(requester, target);
		validateRequesterHasHigherRole(requester, target);
	}

	public void validateRemoveMember(WorkspaceMember requester, WorkspaceMember target) {
		validateNotSelfKickOut(requester, target);
		validateRequesterHasHigherRole(requester, target);
	}

	private void validateNotSelfUpdate(WorkspaceMember requester, WorkspaceMember target) {
		if (requester.getId().equals(target.getId())) {
			throw new InvalidRoleUpdateException("Cannot update own role.");
		}
	}

	private void validateNotSelfKickOut(WorkspaceMember requester, WorkspaceMember target) {
		if (requester.getId().equals(target.getId())) {
			throw new InvalidRoleUpdateException("Cannot kick yourself out.");
		}
	}

	private void validateRequesterHasHigherRole(WorkspaceMember requester, WorkspaceMember target) {
		if (target.getRole().getLevel() >= requester.getRole().getLevel()) {
			throw new InvalidRoleUpdateException("You must have a higher role than the target member.");
		}
	}
}
