package com.tissue.workspace.application.service.command;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.member.application.service.finder.MemberFinder;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.policy.MemberPolicy;
import com.tissue.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.workspace.application.dto.response.WorkspaceCommandResponse;
import com.tissue.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.WorkspaceExceptions;
import com.tissue.workspace.domain.service.WorkspaceKeyGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceCreateService implements WorkspaceCreateUseCase {

	private static final int MAX_RETRIES = 5;

	private final MemberFinder memberFinder;
	private final WorkspaceCommandRepository workspaceCommandRepository;
	private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
	private final WorkspaceMemberFinder workspaceMemberFinder;
	private final MemberPolicy memberPolicy;

	@Override
	@Retryable(
		retryFor = {DataIntegrityViolationException.class},
		notRecoverable = {BadRequestException.class},
		maxAttempts = MAX_RETRIES,
		backoff = @Backoff(delay = 300)
	)
	@Transactional
	public WorkspaceCommandResponse create(CreateWorkspaceCommand cmd) {
		Member member = memberFinder.getActiveBy(cmd.memberId());

		String workspaceKey = WorkspaceKeyGenerator.generateWorkspaceKey();

		int ownedCount = workspaceMemberFinder.countOwnedWorkspacesBy(member);
		int joinedCount = workspaceMemberFinder.countJoinedWorkspacesBy(member);

		memberPolicy.ensureCanCreateWorkspace(ownedCount, joinedCount, member);

		Workspace workspace = Workspace.create(workspaceKey, cmd.name(), cmd.description());
		Workspace savedWorkspace = workspaceCommandRepository.save(workspace);

		WorkspaceMember owner = WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER);
		workspaceMemberCommandRepository.save(owner);

		return WorkspaceCommandResponse.from(savedWorkspace);
	}

	@Recover
	public WorkspaceCommandResponse recover(DataIntegrityViolationException exception, CreateWorkspaceCommand cmd) {
		log.error("Retry failed. Workspace code collision could not be resolved after {} attempts.", MAX_RETRIES);
		throw WorkspaceExceptions.keyGenerationFailed(exception);
	}
}
