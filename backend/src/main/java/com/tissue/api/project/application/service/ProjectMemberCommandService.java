package com.tissue.api.project.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.domain.SelfOperationNotAllowedException;
import com.tissue.api.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.api.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.api.project.application.dto.request.JoinProjectCommand;
import com.tissue.api.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.api.project.application.dto.request.LeaveProjectCommand;
import com.tissue.api.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.api.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.api.project.application.port.in.ProjectMemberCommandUseCase;
import com.tissue.api.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.application.service.finder.ProjectMemberFinder;
import com.tissue.api.project.application.service.validator.ProjectMemberValidator;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;
import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectMemberCommandService implements ProjectMemberCommandUseCase {

	private final ProjectFinder projectFinder;
	private final ProjectMemberFinder projectMemberFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final ProjectMemberValidator projectMemberValidator;
	private final ProjectMemberCommandRepository projectMemberRepository;

	@Override
	public ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());

		Set<Long> targetMemberIds = cmd.extractMemberIds();
		Map<Long, ProjectRole> roleMap = cmd.extractRoleMap();

		List<WorkspaceMember> workspaceMembers = workspaceMemberFinder.findAllBy(
			targetMemberIds,
			cmd.workspaceKey()
		);

		Set<Long> existingMemberIds = projectMemberFinder.findExistingMemberIdsBy(project, targetMemberIds);

		List<ProjectMember> newMembers = new ArrayList<>();

		for (WorkspaceMember wm : workspaceMembers) {
			if (existingMemberIds.contains(wm.getMemberId())) {
				continue;
			}

			ProjectRole role = roleMap.get(wm.getMemberId());
			newMembers.add(ProjectMember.create(project, wm, role));
		}

		projectMemberRepository.saveAll(newMembers);

		// TODO: ProjectMembersAddedEvent

		return ProjectMembersCommandResult.of(project, newMembers);
	}

	@Override
	public ProjectMemberCommandResult join(JoinProjectCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(
			cmd.actorMemberId(),
			cmd.workspaceKey()
		);

		projectMemberValidator.ensureNotAlreadyJoined(project, cmd.actorMemberId());

		ProjectMember projectMember = ProjectMember.create(project, workspaceMember, project.getDefaultJoinRole());
		projectMemberRepository.save(projectMember);

		// TODO: ProjectMemberJoinedEvent

		return ProjectMemberCommandResult.of(projectMember);
	}

	@Override
	public ProjectMemberCommandResult leave(LeaveProjectCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

		actor.remove();

		// TODO: ProjectMemberLeavedEvent

		return ProjectMemberCommandResult.of(actor);
	}

	@Override
	public ProjectMemberCommandResult kickMember(KickProjectMemberCommand cmd) {

		if (cmd.actorMemberId().equals(cmd.targetMemberId())) {
			throw new SelfOperationNotAllowedException("Self kick not allowed. Use project leave instead.");
		}

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		ProjectMember target = projectMemberFinder.findBy(project, cmd.targetMemberId());

		target.remove();

		// TODO: ProjectMemberKickedEvent

		return ProjectMemberCommandResult.of(target);

	}

	@Override
	public ProjectMemberCommandResult changeProjectRole(ChangeProjectRoleCommand cmd) {

		if (cmd.actorMemberId().equals(cmd.targetMemberId())) {
			throw new SelfOperationNotAllowedException("Self project role modification not allowed.");
		}

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		ProjectMember target = projectMemberFinder.findBy(project, cmd.targetMemberId());

		target.changeRole(cmd.newRole());

		// TODO: ProjectMemberRoleChangedEvent

		return ProjectMemberCommandResult.of(target);
	}

	// TODO: UseCase에 포함되지 않고 다른 애플리케이션 서비스에서 호출한다고 주석으로 명시
	//  아예 새로운 클래스(ProjectParticipationService)로 분리할까?
	@Transactional
	public void addMember(Project project, Long memberId, ProjectRole role) {
		if (projectMemberFinder.existsBy(project, memberId)) {
			return;
		}

		WorkspaceMember wm = workspaceMemberFinder.findBy(memberId, project.getWorkspaceKey());

		ProjectMember pm = ProjectMember.create(project, wm, role);
		projectMemberRepository.save(pm);
	}
}
