package com.tissue.api.workspace.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.workspace.application.dto.request.DeleteWorkspaceCommand;
import com.tissue.api.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.api.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;
import com.tissue.api.workspace.application.port.in.WorkspaceCommandUseCase;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService implements WorkspaceCommandUseCase {

	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;

	public WorkspaceCommandResult updateInfo(UpdateWorkspaceInfoCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		Patchers.apply(cmd.name(), workspace::updateName);
		Patchers.apply(cmd.description(), workspace::updateDescription);

		return WorkspaceCommandResult.from(workspace);
	}

	/**
	 * Todo
	 *  - 30일 이상 softDelete 상태인 워크스페이스는 배치(batch)로 삭제
	 */
	public WorkspaceCommandResult delete(DeleteWorkspaceCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());

		// TODO: Workspace 하위 리소스도 cascade로 soft-delete 처리 해야하나? 아니면 그냥 Workspace만 soft-delete?

		workspace.softDelete();

		return WorkspaceCommandResult.from(workspace);
	}

	public WorkspaceCommandResult transferOwnership(TransferOwnershipCommand cmd) {
		Workspace workspace = workspaceFinder.findByKey(cmd.workspaceKey());
		WorkspaceMember originalOwner = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.actorMemberId(),
			workspace);
		WorkspaceMember newOwner = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.targetMemberId(), workspace);

		workspace.transferOwnership(originalOwner, newOwner);

		// TODO: WorkspaceOwnershipTransferredEvent

		return WorkspaceCommandResult.from(workspace);
	}
}
