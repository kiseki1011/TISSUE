package com.tissue.api.workspace.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.team.application.service.command.TeamFinder;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.workspace.application.dto.request.AddPositionCommand;
import com.tissue.api.workspace.application.dto.request.AddTeamCommand;
import com.tissue.api.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.api.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.api.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.api.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;
import com.tissue.api.workspace.application.port.in.WorkspaceMemberCommandUseCase;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.service.WorkspaceSecurityGuard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberCommandService implements WorkspaceMemberCommandUseCase {

	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final PositionFinder positionFinder;
	private final TeamFinder teamFinder;
	private final WorkspaceSecurityGuard workspaceSecurityGuard;
	// private final ApplicationEventPublisher eventPublisher;

	public WorkspaceMemberCommandResult updateDisplayName(UpdateDisplayNameCommand cmd) {

		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		workspaceMember.updateDisplayName(cmd.displayName());

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}

	public WorkspaceMemberCommandResult updateRole(UpdateRoleCommand cmd) {

		WorkspaceMember requester = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(
			cmd.memberId(),
			cmd.workspaceKey()
		);

		WorkspaceMember target = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(
			cmd.targetMemberId(),
			cmd.workspaceKey()
		);

		if (target.isOwner()) {
			throw new RuntimeException("Cannot change role of OWNER. Use ownership transfer.");
		}

		target.changeRoleTo(cmd.role());

		// TODO: WorkspaceMemberRoleChangedEvent

		return WorkspaceMemberCommandResult.from(target);
	}

	public WorkspaceMemberCommandResult addPosition(AddPositionCommand cmd) {

		Position position = positionFinder.findByIdAndWorkspaceKey(
			cmd.positionId(),
			cmd.workspaceKey()
		);

		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(
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

		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(
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

		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(
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

		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		workspaceMember.removeTeam(team);

		return WorkspaceMemberCommandResult.from(workspaceMember);
	}
}
