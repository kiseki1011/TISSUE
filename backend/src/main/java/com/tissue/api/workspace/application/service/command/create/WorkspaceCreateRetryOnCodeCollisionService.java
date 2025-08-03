package com.tissue.api.workspace.application.service.command.create;

import static com.tissue.api.workspacemember.domain.model.WorkspaceMember.*;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InternalServerException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.global.key.WorkspaceKeyGenerator;
import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspace.domain.service.validator.WorkspaceValidator;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceCreateRetryOnCodeCollisionService implements WorkspaceCreateService {
	private static final int MAX_RETRIES = 5;

	private final MemberFinder memberFinder;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final PasswordEncoder passwordEncoder;
	private final WorkspaceValidator workspaceValidator;

	@Override
	@Retryable(
		retryFor = {DataIntegrityViolationException.class},
		notRecoverable = {InvalidOperationException.class},
		maxAttempts = MAX_RETRIES,
		backoff = @Backoff(delay = 300)
	)
	@Transactional
	public WorkspaceResponse createWorkspace(
		CreateWorkspaceRequest request,
		Long memberId
	) {
		Member member = memberFinder.findMemberById(memberId);

		Workspace workspace = CreateWorkspaceRequest.to(request);
		setGeneratedWorkspaceCode(workspace);
		setEncodedPasswordIfPresent(request, workspace);
		setIssueKeyPrefix(request, workspace);

		Workspace savedWorkspace = workspaceRepository.saveAndFlush(workspace);

		workspaceMemberRepository.save(addOwnerWorkspaceMember(
			member,
			savedWorkspace
		));

		return WorkspaceResponse.from(savedWorkspace);
	}

	@Recover
	public WorkspaceResponse recover(
		DataIntegrityViolationException exception,
		CreateWorkspaceRequest request,
		Long memberId
	) {
		log.error("Retry failed. Workspace code collision could not be resolved after {} attempts.", MAX_RETRIES);
		throw new InternalServerException(
			"Failed to solve workspace code collision after " + MAX_RETRIES + " attempts.",
			exception
		);
	}

	private void setIssueKeyPrefix(CreateWorkspaceRequest request, Workspace workspace) {
		workspaceValidator.validateIssueKeyPrefix(request.issueKeyPrefix());
		workspace.updateIssueKeyPrefix(request.issueKeyPrefix());
	}

	private void setGeneratedWorkspaceCode(Workspace workspace) {
		String generatedCode = WorkspaceKeyGenerator.generateWorkspaceKeySuffix();
		workspace.setCode(generatedCode);
	}

	private void setEncodedPasswordIfPresent(CreateWorkspaceRequest request, Workspace workspace) {
		String encodedPassword = Optional.ofNullable(request.password())
			.map(passwordEncoder::encode)
			.orElse(null);
		workspace.updatePassword(encodedPassword);
	}
}
