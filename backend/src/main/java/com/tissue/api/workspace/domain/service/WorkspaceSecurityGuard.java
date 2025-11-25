package com.tissue.api.workspace.domain.service;

import static com.tissue.api.workspace.domain.enums.WorkspaceRole.*;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.exception.WorkspaceMemberNotFoundException;
import com.tissue.api.workspace.domain.port.out.WorkspaceMemberCommandRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceSecurityGuard {

	private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

	public boolean isSelfModification(@NonNull Long targetMemberId, @NonNull Long actorMemberId) {
		return targetMemberId.equals(actorMemberId);
	}

	public boolean isMember(@NonNull String workspaceKey, @NonNull Long actorMemberId) {
		return findWorkspaceMember(actorMemberId, workspaceKey)
			.map(actor -> actor.roleIsEqualOrHigherThan(MEMBER))
			.orElse(false);
	}

	public boolean isAdmin(@NonNull String workspaceKey, @NonNull Long actorMemberId) {
		return findWorkspaceMember(actorMemberId, workspaceKey)
			.map(actor -> actor.roleIsEqualOrHigherThan(ADMIN))
			.orElse(false);
	}

	public boolean isOwner(@NonNull String workspaceKey, @NonNull Long actorMemberId) {
		return findWorkspaceMember(actorMemberId, workspaceKey)
			.map(actor -> actor.roleIsEqualOrHigherThan(OWNER))
			.orElse(false);
	}

	public boolean targetHasLowerRole(
		@NonNull String workspaceKey,
		@NonNull Long targetMemberId,
		@NonNull Long actorMemberId
	) {
		if (targetMemberId.equals(actorMemberId)) {
			return false;
		}

		WorkspaceMember actor = findWorkspaceMember(actorMemberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(actorMemberId, workspaceKey));

		WorkspaceMember target = findWorkspaceMember(targetMemberId, workspaceKey)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(targetMemberId, workspaceKey));

		return target.roleIsLowerThan(actor.getRole());
	}

	private Optional<WorkspaceMember> findWorkspaceMember(Long memberId, String workspaceKey) {
		return workspaceMemberCommandRepository.findByMember_IdAndWorkspaceKey(
			memberId,
			workspaceKey
		);
	}
}
