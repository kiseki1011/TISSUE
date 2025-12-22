package com.tissue.workspace.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.position.application.service.finder.PositionFinder;
import com.tissue.position.domain.Position;
import com.tissue.team.application.service.finder.TeamFinder;
import com.tissue.team.domain.Team;
import com.tissue.workspace.application.dto.request.AddPositionCommand;
import com.tissue.workspace.application.dto.request.AddTeamCommand;
import com.tissue.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceMemberManageService implements WorkspaceMemberManageUseCase {

	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final PositionFinder positionFinder;
	private final TeamFinder teamFinder;
	// private final ApplicationEventPublisher eventPublisher;

	@Override
	public void updateDisplayName(UpdateDisplayNameCommand cmd) {
		Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
		// TODO: pass workspace instead of workspaceKey
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());
		workspaceMember.updateDisplayName(cmd.displayName());
	}

	@Override
	public void updateRole(UpdateRoleCommand cmd) {
		Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
		// TODO: pass workspace instead of workspaceKey
		WorkspaceMember requester = workspaceMemberFinder.findBy(cmd.memberId(), cmd.workspaceKey());
		WorkspaceMember target = workspaceMemberFinder.findBy(cmd.targetMemberId(), cmd.workspaceKey());

		target.changeRoleTo(cmd.role());

		// TODO: WorkspaceMemberRoleChangedEvent
	}

	@Override
	public void addPosition(AddPositionCommand cmd) {
		Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
		Position position = positionFinder.getBy(cmd.positionId(), workspace);
		// TODO: pass workspace instead of workspaceKey
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.addPosition(position);

		// TODO: WorkspaceMemberPositionChangedEvent
	}

	@Override
	public void removePosition(RemovePositionCommand cmd) {
		Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
		Position position = positionFinder.getBy(cmd.positionId(), workspace);
		// TODO: pass workspace instead of workspaceKey
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.removePosition(position);

		// TODO: WorkspaceMemberPositionChangedEvent
	}

	@Override
	public void addTeam(AddTeamCommand cmd) {
		Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
		Team team = teamFinder.getBy(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.addTeam(team);

		// TODO: WorkspaceMemberTeamChangedEvent
	}

	@Override
	public void removeTeam(RemoveTeamCommand cmd) {
		Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
		Team team = teamFinder.getBy(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		workspaceMember.removeTeam(team);

		// TODO: WorkspaceMemberTeamChangedEvent
	}
}
