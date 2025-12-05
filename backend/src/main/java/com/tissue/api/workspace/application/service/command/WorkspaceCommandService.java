package com.tissue.api.workspace.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.workspace.application.dto.request.DeleteWorkspaceCommand;
import com.tissue.api.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.api.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.api.workspace.application.port.in.WorkspaceCommandUseCase;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceCommandService implements WorkspaceCommandUseCase {

	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;

	public void updateInfo(UpdateWorkspaceInfoCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		Patchers.apply(cmd.name(), workspace::updateName);
		Patchers.apply(cmd.description(), workspace::updateDescription);
	}

	/**
	 * Todo
	 *  - 30일 이상 softDelete 상태인 워크스페이스는 배치(batch)로 삭제
	 */
	public void delete(DeleteWorkspaceCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		workspace.softDelete();
	}

	public void transferOwnership(TransferOwnershipCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());
		WorkspaceMember originalOwner = workspaceMemberFinder.findBy(
			cmd.actorMemberId(),
			workspace
		);
		WorkspaceMember newOwner = workspaceMemberFinder.findBy(
			cmd.targetMemberId(),
			workspace
		);

		// TODO: transferOwnership의 주석 참고
		workspace.transferOwnership(originalOwner, newOwner);

		// TODO: WorkspaceOwnershipTransferredEvent
	}
}
