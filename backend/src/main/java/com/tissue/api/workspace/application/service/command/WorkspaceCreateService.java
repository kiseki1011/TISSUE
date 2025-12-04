package com.tissue.api.workspace.application.service.command;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.base.BadRequestException;
import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResponse;
import com.tissue.api.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.api.workspace.application.port.out.WorkspaceCommandRepository;
import com.tissue.api.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.WorkspaceMember;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;
import com.tissue.api.workspace.domain.service.WorkspaceKeyGenerator;

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
	// private final MemberPolicy memberPolicy;

	@Override
	@Retryable(
		retryFor = {DataIntegrityViolationException.class},
		notRecoverable = {BadRequestException.class},
		maxAttempts = MAX_RETRIES,
		backoff = @Backoff(delay = 300)
	)
	public WorkspaceCommandResponse create(CreateWorkspaceCommand cmd) {

		Member member = memberFinder.findMemberById(cmd.memberId());

		String workspaceKey = WorkspaceKeyGenerator.generateWorkspaceKey();

		// memberPolicy.ensureCanCreateWorkspace(currentOwnedWorkspaces, currentJoinedWorkspaces); // 내부에 ensureCanJoinWorkspace() 호출

		Workspace workspace = Workspace.create(workspaceKey, cmd.name(), cmd.description());
		Workspace savedWorkspace = workspaceCommandRepository.save(workspace);

		WorkspaceMember owner = WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER);
		workspaceMemberCommandRepository.save(owner);

		return WorkspaceCommandResponse.from(savedWorkspace);
	}

	@Recover
	public WorkspaceCommandResponse recover(DataIntegrityViolationException exception, CreateWorkspaceCommand cmd) {
		log.error("Retry failed. Workspace code collision could not be resolved after {} attempts.", MAX_RETRIES);
		// TODO: WorkspaceKeyCollisionException extends InternalServerException
		throw new RuntimeException(
			"Failed to solve workspace code collision after %d attempts.".formatted(MAX_RETRIES),
			exception
		);
	}
}
