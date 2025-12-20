package com.tissue.project.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.JoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.project.application.port.in.ProjectMemberCommandUseCase;
import com.tissue.project.application.port.out.ProjectMemberCommandRepository;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.application.service.validator.ProjectValidator;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.exception.ProjectExceptions;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectMemberCommandService implements ProjectMemberCommandUseCase {

	private final ProjectFinder projectFinder;
	private final ProjectMemberFinder projectMemberFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final ProjectValidator projectValidator;
	private final ProjectMemberCommandRepository projectMemberRepository;

	@Override
	@Transactional
	public ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());

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
	@Transactional
	public ProjectMemberCommandResult join(JoinProjectCommand cmd) {
		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		WorkspaceMember workspaceMember = workspaceMemberFinder.findBy(cmd.actorMemberId(), cmd.workspaceKey());

		projectValidator.ensureNotAlreadyJoined(project, cmd.actorMemberId());

		ProjectMember projectMember = ProjectMember.create(project, workspaceMember, project.getDefaultJoinRole());
		projectMemberRepository.save(projectMember);

		// TODO: ProjectMemberJoinedEvent

		return ProjectMemberCommandResult.of(projectMember);
	}

	@Override
	@Transactional
	public ProjectMemberCommandResult leave(String workspaceKey, String projectKey, Long memberId) {
		Project project = projectFinder.getModifiableBy(projectKey, workspaceKey);
		ProjectMember actor = projectMemberFinder.findBy(project, memberId);

		actor.remove();

		// TODO: ProjectMemberLeavedEvent

		return ProjectMemberCommandResult.of(actor);
	}

	@Override
	@Transactional
	public ProjectMemberCommandResult kickMember(KickProjectMemberCommand cmd) {
		if (cmd.actorMemberId().equals(cmd.targetMemberId())) {
			throw ProjectExceptions.selfKick();
		}

		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		ProjectMember target = projectMemberFinder.findBy(project, cmd.targetMemberId());

		target.remove();

		// TODO: ProjectMemberKickedEvent

		return ProjectMemberCommandResult.of(target);

	}

	@Override
	@Transactional
	public ProjectMemberCommandResult changeProjectRole(ChangeProjectRoleCommand cmd) {
		if (cmd.actorMemberId().equals(cmd.targetMemberId())) {
			throw ProjectExceptions.selfRole();
		}

		Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
		ProjectMember target = projectMemberFinder.findBy(project, cmd.targetMemberId());

		target.changeRole(cmd.newRole());

		// TODO: ProjectMemberRoleChangedEvent

		return ProjectMemberCommandResult.of(target);
	}

	// TODO: add javadoc about the next information
	//  - is not a UseCase
	//  - is called from another service(internal usage)
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
