package com.tissue.api.workspacemember.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.ForbiddenOperationException;
import com.tissue.api.common.exception.InvalidOperationException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

/**
 * Todo
 *  - 모든 검증을 WorkspaceMember 엔티티에 정의해서 사용할까?
 *    - 현재 레포지토리 호출, 또는 굉장히 복잡한 비즈니스 로직 검증은 없음
 */
@Component
@RequiredArgsConstructor
public class WorkspaceMemberValidator {

	public void validateRoleUpdate(WorkspaceMember requester, WorkspaceMember target) {
		validateNotSelfUpdate(requester, target);
		validateRequesterHasHigherRole(requester, target);
	}

	/**
	 * Todo
	 *  - 본인이 자발적으로 나가는 경우를 위해 스스로 강퇴하는 것을 허용하도록 변경
	 *  - validateNotSelfKickOut 제거 하면 될 듯
	 */
	public void validateRemoveMember(WorkspaceMember requester, WorkspaceMember target) {
		validateNotSelfKickOut(requester, target);
		validateRequesterHasHigherRole(requester, target);
	}

	private void validateNotSelfUpdate(WorkspaceMember requester, WorkspaceMember target) {
		if (requester.getId().equals(target.getId())) {
			throw new InvalidOperationException("Cannot update own role.");
		}
	}

	private void validateNotSelfKickOut(WorkspaceMember requester, WorkspaceMember target) {
		if (requester.getId().equals(target.getId())) {
			throw new InvalidOperationException("Cannot kick yourself out.");
		}
	}

	/**
	 * Todo
	 *  - WorkspaceRole의 메서드를 사용해서 비교하기
	 */
	private void validateRequesterHasHigherRole(WorkspaceMember requester, WorkspaceMember target) {
		if (target.getRole().getLevel() >= requester.getRole().getLevel()) {
			throw new ForbiddenOperationException(
				String.format("You must have a higher role than the target member. requesterRole: %s, targetRole: %s",
					requester.getRole(), target.getRole()));
		}
	}
}
