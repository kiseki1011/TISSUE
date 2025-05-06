package com.tissue.api.workspace.service.command.create;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InternalServerException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.service.command.MemberReader;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.util.RandomNicknameGenerator;
import com.tissue.api.util.WorkspaceCodeGenerator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;
import com.tissue.api.workspace.validator.WorkspaceValidator;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DB에서 발생하는 예외를 서비스 계층에서 잡아서 핸들링 로직(워크스페이스 코드 재생성) 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceCreateRetryOnCodeCollisionService implements WorkspaceCreateService {
	private static final int MAX_RETRIES = 5;

	private final MemberReader memberReader;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;
	private final RandomNicknameGenerator randomNicknameGenerator;
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
		Member member = memberReader.findMember(memberId);

		Workspace workspace = CreateWorkspaceRequest.to(request);
		setWorkspaceCode(workspace);
		setEncodedPasswordIfPresent(request, workspace);
		setKeyPrefix(request, workspace);

		Workspace savedWorkspace = workspaceRepository.saveAndFlush(workspace);

		WorkspaceMember workspaceMember = WorkspaceMember.addOwnerWorkspaceMember(
			member,
			savedWorkspace,
			randomNicknameGenerator.generateNickname()
		);
		workspaceMemberRepository.save(workspaceMember);

		return WorkspaceResponse.from(savedWorkspace);
	}

	@Recover
	public WorkspaceResponse recover(
		DataIntegrityViolationException exception,
		CreateWorkspaceRequest request,
		Long memberId
	) {
		log.error("Retry failed. Workspace code collision could not be resolved after {} attempts.", MAX_RETRIES);
		// TODO: InternalServerException에 exception도 받을수 있도록 수정
		throw new InternalServerException(
			String.format("Failed to solve workspace code collision. Max retry limit: %d", MAX_RETRIES)
		);
	}

	private void setKeyPrefix(CreateWorkspaceRequest request, Workspace workspace) {
		workspaceValidator.validateIssueKeyPrefix(request.issueKeyPrefix());
		workspace.updateIssueKeyPrefix(request.issueKeyPrefix());
	}

	private void setWorkspaceCode(Workspace workspace) {
		String generatedCode = workspaceCodeGenerator.generateWorkspaceCode();
		workspace.setCode(generatedCode);
	}

	private void setEncodedPasswordIfPresent(CreateWorkspaceRequest request, Workspace workspace) {
		String encodedPassword = Optional.ofNullable(request.password())
			.map(passwordEncoder::encode)
			.orElse(null);
		workspace.updatePassword(encodedPassword);
	}
}
