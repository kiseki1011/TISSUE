package com.tissue.api.workspacemember.domain.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberAuthorizationService {

	private final WorkspaceMemberReader workspaceMemberReader;

	public void validateCanRemoveWorkspaceMember(
		Long requesterMemberId,
		Long targetMemberId,
		String workspaceCode
	) {
		if (isSelf(requesterMemberId, targetMemberId)) {
			throw new InvalidOperationException(
				"Cannot remove yourself. Use DELETE api/v1/workspaces/" + workspaceCode + "/members"
			);
		}

		validateRequesterHasHigherRole(requesterMemberId, targetMemberId, workspaceCode);
	}

	// TODO: 권한을 하드 코딩 x, 더 괜찮은 방법 찾기
	public void validateCanLeaveWorkspace(
		Long memberId,
		String workspaceCode
	) {
		WorkspaceMember workspaceMember = workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode);
		if (workspaceMember.getRole() == WorkspaceRole.OWNER) {
			throw new InvalidOperationException("You cannot leave workspace if you are the OWNER.");
		}
	}

	public void validateCanUpdateRole(
		Long requesterMemberId,
		Long targetMemberId,
		String workspaceCode
	) {
		if (isSelf(requesterMemberId, targetMemberId)) {
			throw new InvalidOperationException("Cannot update your own role");
		}

		validateRequesterHasHigherRole(requesterMemberId, targetMemberId, workspaceCode);
	}

	public void validateCanModifyTeamPosition(
		Long requesterMemberId,
		Long targetMemberId,
		String workspaceCode
	) {
		if (isSelf(requesterMemberId, targetMemberId)) {
			return;
		}

		if (hasInsufficientPermission(requesterMemberId, workspaceCode)) {
			throw new ForbiddenOperationException("MANAGER 이상의 권한이 필요합니다.");
		}
	}

	private void validateRequesterHasHigherRole(Long requesterMemberId, Long targetMemberId, String workspaceCode) {

		WorkspaceMember target = workspaceMemberReader.findWorkspaceMember(targetMemberId, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterMemberId, workspaceCode);

		if (target.getRole().getLevel() >= requester.getRole().getLevel()) {
			throw new ForbiddenOperationException(
				String.format("You must have a higher role than the target. your role: %s, target's role: %s",
					requester.getRole(), target.getRole())
			);
		}
	}

	private boolean isSelf(Long requesterMemberId, Long targetMemberId) {
		return Objects.equals(requesterMemberId, targetMemberId);
	}

	// TODO: 권한을 하드 코딩 x, 더 괜찮은 방법 찾기
	private boolean hasInsufficientPermission(Long memberId, String workspaceCode) {
		return workspaceMemberReader.findWorkspaceMember(memberId, workspaceCode)
			.roleIsLowerThan(WorkspaceRole.MANAGER);
	}
}
