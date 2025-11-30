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
import com.tissue.api.workspace.application.dto.response.WorkspaceCommandResult;
import com.tissue.api.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.application.port.out.WorkspaceCommandRepository;
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
	// private final MemberPolicy memberPolicy;

	@Override
	@Retryable(
		retryFor = {DataIntegrityViolationException.class},
		notRecoverable = {BadRequestException.class},
		maxAttempts = MAX_RETRIES,
		backoff = @Backoff(delay = 300)
	)
	public WorkspaceCommandResult create(CreateWorkspaceCommand cmd) {

		Member member = memberFinder.findMemberById(cmd.memberId());

		String workspaceKey = WorkspaceKeyGenerator.generateWorkspaceKey();

		// TODO: 추가 필요
		// workspaceValidator.ensureKeyIsUnique(workspaceKey);

		// memberPolicy.ensureCanCreateWorkspace();

		Workspace workspace = Workspace.create(
			workspaceKey,
			cmd.name(),
			cmd.description(),
			member
		);

		// TODO: saveAndFlush가 꼭 필요한가?
		//  내 기억상에는 일부러 flush를 통해 key의 유일성 검사(DB 유일성 제약을 통해)를 유도하려고 했던 것 같음.
		Workspace savedWorkspace = workspaceCommandRepository.save(workspace);

		return WorkspaceCommandResult.from(savedWorkspace);
	}

	@Recover
	public WorkspaceCommandResult recover(DataIntegrityViolationException exception, CreateWorkspaceCommand cmd) {
		log.error("Retry failed. Workspace code collision could not be resolved after {} attempts.", MAX_RETRIES);
		// TODO: WorkspaceKeyCollisionException extends InternalServerException
		throw new RuntimeException(
			"Failed to solve workspace code collision after %d attempts.".formatted(MAX_RETRIES),
			exception
		);
	}
}
