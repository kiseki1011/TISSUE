package com.tissue.api.workspace.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.team.application.service.command.TeamFinder;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.application.dto.request.AssignPositionCommand;
import com.tissue.api.workspace.application.dto.request.AssignTeamCommand;
import com.tissue.api.workspace.application.dto.request.RemovePositionCommand;
import com.tissue.api.workspace.application.dto.request.RemoveTeamCommand;
import com.tissue.api.workspace.application.dto.request.RemoveWorkspaceMemberCommand;
import com.tissue.api.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.api.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.api.workspace.application.dto.request.UpdateRoleCommand;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.service.WorkspaceMemberPermissionValidator;
import com.tissue.api.workspace.domain.service.WorkspaceMemberValidator;
import com.tissue.api.workspace.domain.port.out.WorkspaceMemberRepository;
import com.tissue.api.workspace.adapter.in.web.dto.response.WorkspaceMemberResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final PositionFinder positionFinder;
	private final TeamFinder teamFinder;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberValidator workspaceMemberValidator;
	private final WorkspaceMemberPermissionValidator workspaceMemberPermissionValidator;
	// private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public WorkspaceMemberResponse updateDisplayName(UpdateDisplayNameCommand cmd) {
		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.memberId(),
			cmd.workspaceKey());

		workspaceMember.updateDisplayName(cmd.displayName());

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse updateRole(UpdateRoleCommand cmd) {
		WorkspaceMember requester = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.memberId(),
			cmd.workspaceKey());
		WorkspaceMember target = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.targetMemberId(),
			cmd.workspaceKey());

		workspaceMemberPermissionValidator.validateCanUpdateRole(requester, target);

		target.changeRoleTo(cmd.role());

		return WorkspaceMemberResponse.from(target);
	}

	@Transactional
	public WorkspaceMemberResponse assignPosition(AssignPositionCommand cmd) {
		Position position = positionFinder.findPosition(cmd.positionId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.memberId(),
			cmd.workspaceKey());

		workspaceMember.addPosition(position);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse removePosition(RemovePositionCommand cmd) {
		Position position = positionFinder.findPosition(cmd.positionId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.memberId(),
			cmd.workspaceKey());

		workspaceMember.removePosition(position);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse assignTeam(AssignTeamCommand cmd) {
		Team team = teamFinder.findTeam(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.memberId(),
			cmd.workspaceKey());

		workspaceMember.addTeam(team);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse removeTeam(RemoveTeamCommand cmd) {
		Team team = teamFinder.findTeam(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findByMemberIdAndWorkspaceKey(cmd.memberId(),
			cmd.workspaceKey());

		workspaceMember.removeTeam(team);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse transferOwnership(TransferOwnershipCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		WorkspaceMember requester = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.memberId(), workspace);
		WorkspaceMember target = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.targetMemberId(), workspace);

		workspace.transferOwnership(requester, target);

		// TODO: WorkspaceOwnershipTransferredEvent

		return WorkspaceMemberResponse.from(target);
	}

	@Transactional
	public void removeWorkspaceMember(RemoveWorkspaceMemberCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		WorkspaceMember requester = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.memberId(), workspace);
		WorkspaceMember target = workspaceMemberFinder.findByMemberIdAndWorkspace(cmd.targetMemberId(), workspace);

		workspaceMemberPermissionValidator.validateCanRemoveWorkspaceMember(requester, target);

		// TODO: WorkspaceMemberKickedEvent

		workspace.removeMember(target);
	}
}
