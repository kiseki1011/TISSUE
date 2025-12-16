package com.tissue.workspace.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.position.application.service.command.PositionFinder;
import com.tissue.position.domain.model.Position;
import com.tissue.team.application.service.command.TeamFinder;
import com.tissue.team.domain.model.Team;
import com.tissue.workspace.application.dto.request.AddPositionCommand;
import com.tissue.workspace.application.dto.request.AddTeamCommand;
import com.tissue.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberManageService implements WorkspaceMemberManageUseCase {

	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final PositionFinder positionFinder;
	private final TeamFinder teamFinder;
	// private final ApplicationEventPublisher eventPublisher;

	public void updateDisplayName(UpdateDisplayNameCommand cmd) {
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());
		workspaceMember.updateDisplayName(cmd.displayName());
	}

	public void updateRole(UpdateRoleCommand cmd) {
		WorkspaceMember requester = workspaceMemberFinder.findBy(cmd.memberId(), cmd.workspaceKey());
		WorkspaceMember target = workspaceMemberFinder.findBy(cmd.targetMemberId(), cmd.workspaceKey());

		target.changeRoleTo(cmd.role());

		// TODO: WorkspaceMemberRoleChangedEvent
	}

	public void addPosition(AddPositionCommand cmd) {
		Position position = positionFinder.findByIdAndWorkspaceKey(cmd.positionId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.addPosition(position);
	}

	public void removePosition(RemovePositionCommand cmd) {
		Position position = positionFinder.findByIdAndWorkspaceKey(cmd.positionId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.removePosition(position);
	}

	public void addTeam(AddTeamCommand cmd) {
		Team team = teamFinder.findByIdAndWorkspaceKey(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.addTeam(team);
	}

	public void removeTeam(RemoveTeamCommand cmd) {
		Team team = teamFinder.findByIdAndWorkspaceKey(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.removeTeam(team);
	}
}
