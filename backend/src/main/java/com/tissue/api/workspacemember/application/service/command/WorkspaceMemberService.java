package com.tissue.api.workspacemember.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.position.application.service.command.PositionFinder;
import com.tissue.api.position.domain.model.Position;
import com.tissue.api.team.application.service.command.TeamFinder;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.workspacemember.application.dto.AssignPositionCommand;
import com.tissue.api.workspacemember.application.dto.AssignTeamCommand;
import com.tissue.api.workspacemember.application.dto.RemovePositionCommand;
import com.tissue.api.workspacemember.application.dto.RemoveTeamCommand;
import com.tissue.api.workspacemember.application.dto.RemoveWorkspaceMemberCommand;
import com.tissue.api.workspacemember.application.dto.TransferOwnershipCommand;
import com.tissue.api.workspacemember.application.dto.UpdateDisplayNameCommand;
import com.tissue.api.workspacemember.application.dto.UpdateRoleCommand;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.service.WorkspaceMemberPermissionValidator;
import com.tissue.api.workspacemember.domain.service.WorkspaceMemberValidator;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.presentation.dto.response.WorkspaceMemberResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final PositionFinder positionFinder;
	private final TeamFinder teamFinder;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberValidator workspaceMemberValidator;
	private final WorkspaceMemberPermissionValidator workspaceMemberPermissionValidator;
	// private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public WorkspaceMemberResponse updateDisplayName(UpdateDisplayNameCommand cmd) {
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());

		workspaceMember.updateDisplayName(cmd.displayName());

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse updateRole(UpdateRoleCommand cmd) {
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(cmd.targetMemberId(), cmd.workspaceKey());

		// TODO: validateCanUpdateRole checks the WorkspaceRole hierarchy or if the target is yourself.
		//  Should I move this permission related logic to the controller?
		//  Or is there a way to use Spring-security for this?
		workspaceMemberPermissionValidator.validateCanUpdateRole(requester, target);

		target.updateRole(cmd.role());

		return WorkspaceMemberResponse.from(target);
	}

	@Transactional
	public WorkspaceMemberResponse assignPosition(AssignPositionCommand cmd) {
		Position position = positionFinder.findPosition(cmd.positionId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());

		workspaceMember.addPosition(position);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse removePosition(RemovePositionCommand cmd) {
		Position position = positionFinder.findPosition(cmd.positionId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());

		workspaceMember.removePosition(position);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse assignTeam(AssignTeamCommand cmd) {
		Team team = teamFinder.findTeam(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());

		workspaceMember.addTeam(team);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse removeTeam(RemoveTeamCommand cmd) {
		Team team = teamFinder.findTeam(cmd.teamId(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());

		workspaceMember.removeTeam(team);

		return WorkspaceMemberResponse.from(workspaceMember);
	}

	@Transactional
	public WorkspaceMemberResponse transferOwnership(TransferOwnershipCommand cmd) {
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(cmd.targetMemberId(), cmd.workspaceKey());

		requester.updateRoleToAdmin();
		target.updateRoleToOwner();

		return WorkspaceMemberResponse.from(target);
	}

	@Transactional
	public void removeWorkspaceMember(RemoveWorkspaceMemberCommand cmd) {
		WorkspaceMember requester = workspaceMemberFinder.findWorkspaceMember(cmd.memberId(), cmd.workspaceKey());
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(cmd.targetMemberId(), cmd.workspaceKey());

		workspaceMemberPermissionValidator.validateCanRemoveWorkspaceMember(requester, target);

		target.remove();
	}
}
