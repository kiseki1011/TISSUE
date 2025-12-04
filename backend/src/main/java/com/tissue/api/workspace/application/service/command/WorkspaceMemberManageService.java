package com.tissue.api.workspace.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.security.authorization.WorkspaceSecurityGuard;
import com.tissue.api.team.application.service.command.TeamFinder;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.workspace.application.dto.request.AddPositionCommand;
import com.tissue.api.workspace.application.dto.request.AddTeamCommand;
import com.tissue.api.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.api.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.api.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.api.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;
import com.tissue.api.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberManageService implements WorkspaceMemberManageUseCase {

	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final PositionFinder positionFinder;
	private final TeamFinder teamFinder;
	private final WorkspaceSecurityGuard workspaceSecurityGuard;
	// private final ApplicationEventPublisher eventPublisher;

	public WorkspaceMemberCommandResult updateDisplayName(UpdateDisplayNameCommand cmd) {

		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		workspaceMember.updateDisplayName(cmd.displayName());

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}

	public WorkspaceMemberCommandResult updateRole(UpdateRoleCommand cmd) {

		WorkspaceMember requester = workspaceMemberFinder.findBy(
			cmd.memberId(),
			cmd.workspaceKey()
		);

		WorkspaceMember target = workspaceMemberFinder.findBy(
			cmd.targetMemberId(),
			cmd.workspaceKey()
		);

		target.changeRoleTo(cmd.role());

		// TODO: WorkspaceMemberRoleChangedEvent

		return WorkspaceMemberCommandResult.from(target);
	}

	public WorkspaceMemberCommandResult addPosition(AddPositionCommand cmd) {

		Position position = positionFinder.findByIdAndWorkspaceKey(
			cmd.positionId(),
			cmd.workspaceKey()
		);

		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		workspaceMember.addPosition(position);

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}

	public WorkspaceMemberCommandResult removePosition(RemovePositionCommand cmd) {

		Position position = positionFinder.findByIdAndWorkspaceKey(
			cmd.positionId(),
			cmd.workspaceKey()
		);

		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		workspaceMember.removePosition(position);

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}

	public WorkspaceMemberCommandResult addTeam(AddTeamCommand cmd) {

		Team team = teamFinder.findByIdAndWorkspaceKey(
			cmd.teamId(),
			cmd.workspaceKey()
		);

		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		workspaceMember.addTeam(team);

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}

	public WorkspaceMemberCommandResult removeTeam(RemoveTeamCommand cmd) {

		Team team = teamFinder.findByIdAndWorkspaceKey(
			cmd.teamId(),
			cmd.workspaceKey()
		);

		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		workspaceMember.removeTeam(team);

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}
}
