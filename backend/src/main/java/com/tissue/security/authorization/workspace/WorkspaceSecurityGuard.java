package com.tissue.security.authorization.workspace;

import static com.tissue.workspace.domain.enums.WorkspaceRole.*;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;

// TODO: 권한 redis 캐싱
@Component
@RequiredArgsConstructor
public class WorkspaceSecurityGuard {

	private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

	public boolean isSelfModification(
		Long targetMemberId,
		Long memberId
	) {
		return targetMemberId.equals(memberId);
	}

	public boolean isMember(
		String workspaceKey,
		Long memberId
	) {
		return findWorkspaceMember(memberId, workspaceKey)
			.map(actor -> actor.roleIsEqualOrHigherThan(MEMBER))
			.orElse(false);
	}

	public boolean isAdmin(
		String workspaceKey,
		Long memberId
	) {
		return findWorkspaceMember(memberId, workspaceKey)
			.map(actor -> actor.roleIsEqualOrHigherThan(ADMIN))
			.orElse(false);
	}

	public boolean isOwner(
		String workspaceKey,
		Long memberId
	) {
		return findWorkspaceMember(memberId, workspaceKey)
			.map(actor -> actor.roleIsEqualOrHigherThan(OWNER))
			.orElse(false);
	}

	public boolean targetHasLowerRole(
		String workspaceKey,
		Long targetMemberId,
		Long memberId
	) {
		if (targetMemberId.equals(memberId)) {
			return false;
		}

		WorkspaceMember actor = findWorkspaceMember(memberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(memberId, workspaceKey));

		WorkspaceMember target = findWorkspaceMember(targetMemberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(targetMemberId, workspaceKey));

		return target.roleIsLowerThan(actor.getRole());
	}

	// TODO: OWNER는 Transfer Ownership을 통해서만 가능하다는 걸 주석으로 추가
	public boolean canGrantRole(
		String workspaceKey,
		Long memberId,
		WorkspaceRole grantRole
	) {
		if (grantRole == WorkspaceRole.OWNER) {
			return false;
		}

		return findWorkspaceMember(memberId, workspaceKey)
			.map(actor -> actor.roleIsEqualOrHigherThan(grantRole))
			.orElse(false);
	}

	// TODO: findRoleByMemberIdAndWorkspaceKey 추가 후 사용
	private Optional<WorkspaceMember> findWorkspaceMember(Long memberId, String workspaceKey) {
		return workspaceMemberQueryRepository.findByMember_IdAndWorkspaceKey(
			memberId,
			workspaceKey
		);
	}
}
