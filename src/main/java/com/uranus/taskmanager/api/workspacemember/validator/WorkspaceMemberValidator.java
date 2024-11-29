package com.uranus.taskmanager.api.workspacemember.validator;

import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.InvalidRoleUpdateException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberValidator {

	public void validateRoleUpdate(WorkspaceMember requester, WorkspaceMember target) {
		validateNotSelfUpdate(requester, target);
		validateRequesterHasHigherRole(requester, target);
	}

	private void validateNotSelfUpdate(WorkspaceMember requester, WorkspaceMember target) {
		if (requester.getId().equals(target.getId())) {
			throw new InvalidRoleUpdateException("Cannot update own role.");
		}
	}

	private void validateRequesterHasHigherRole(WorkspaceMember requester, WorkspaceMember target) {
		if (target.getRole().getLevel() >= requester.getRole().getLevel()) {
			throw new InvalidRoleUpdateException("Cannot update role of member with higher or equal role.");
		}
	}
}
