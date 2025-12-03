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

@Service
@RequiredArgsConstructor
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

		// TODO: Workspace 하위 리소스 soft-delete 전략 정하기
		//  전략 1: Workspace 하위 리소스도 cascade로 soft-delete 처리
		//   - 전략 1은 만약 하게 된다면 비동기로 수행하고, 어떻게 정합성을 보장할지 고민 필요
		//   - 그리고 만약 복구(restore) 하는 경우 어떻게 할지 고민해야 함
		//  전략 2: Workspace만 soft-delete
		//   - 전략 2의 경우에는 하위 리소스 조회 시 무조건 "Workspace 조회 -> 하위 리소스 조회에 Workspace 객체 사용" 하는 형태로 설계 필요
		//  그 대신 soft-delete과 복구(restore) 구현 자체는 쉽고, 작업 자체가 가벼움
		workspace.softDelete();

		return WorkspaceCommandResult.from(workspace);
	}

	public WorkspaceCommandResult transferOwnership(TransferOwnershipCommand cmd) {
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

		return WorkspaceCommandResult.from(workspace);
	}
}
