package com.tissue.api.workspacemember.domain.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberValidator {

	// public void validatePositionBelongsToWorkspace(Position position, String workspaceKey) {
	// 	boolean workspaceMismatch = !Objects.equals(position.getWorkspaceKey(), workspaceKey);
	//
	// 	if (workspaceMismatch) {
	// 		throw new InvalidOperationException(
	// 			"Position '%s' (ID: %d) does not belong to workspace '%s'"
	// 				.formatted(position.getName(), position.getId(), workspaceKey)
	// 		);
	// 	}
	// }

	public void validateNotAlreadyAssignedPosition(WorkspaceMember workspaceMember, Position position) {
		boolean alreadyAssigned = workspaceMember.getWorkspaceMemberPositions().stream()
			.anyMatch(wmp -> Objects.equals(wmp.getPosition().getId(), position.getId()));

		if (alreadyAssigned) {
			throw new InvalidOperationException(
				"Position '%s' (ID: %d) is already assigned to member (workspaceKey: %s, memberId: %d)"
					.formatted(position.getName(), position.getId(), workspaceMember.getWorkspaceKey(),
						workspaceMember.getMemberId())
			);
		}
	}

	public void validatePositionIsAssigned(WorkspaceMember workspaceMember, Position position) {
		boolean notAssigned = workspaceMember.getWorkspaceMemberPositions().stream()
			.noneMatch(wmp -> Objects.equals(wmp.getPosition().getId(), position.getId()));

		if (notAssigned) {
			throw new InvalidOperationException(
				"Position '%s' (ID: %d) is not assigned to member (workspaceKey: %s, memberId: %d)"
					.formatted(position.getName(), position.getId(), workspaceMember.getWorkspaceKey(),
						workspaceMember.getMemberId())
			);
		}
	}
}
