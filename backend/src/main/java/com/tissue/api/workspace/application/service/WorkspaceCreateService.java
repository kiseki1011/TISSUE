package com.tissue.api.workspace.application.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.base.BadRequestException;
import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.policy.WorkspacePolicy;
import com.tissue.api.workspace.domain.WorkspaceKeyGenerator;
import com.tissue.api.workspace.domain.port.out.WorkspaceRepository;
import com.tissue.api.workspace.adapter.in.web.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.adapter.in.web.dto.response.WorkspaceResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceCreateService
	implements WorkspaceCreateUseCase { // TODO: WorkspaceCreateService -> WorkspaceCreateUseCase
	private static final int MAX_RETRIES = 5;

	private final MemberFinder memberFinder;
	private final WorkspaceRepository workspaceRepository;
	private final PasswordEncoder passwordEncoder;
	// private final WorkspaceValidator workspaceValidator;
	private final WorkspacePolicy workspacePolicy;

	// TODO: CreateWorkspaceCommand 사용하기
	// TODO: 가독성 리팩토링
	@Override
	@Retryable(
		retryFor = {DataIntegrityViolationException.class},
		notRecoverable = {BadRequestException.class},
		maxAttempts = MAX_RETRIES,
		backoff = @Backoff(delay = 300)
	)
	@Transactional
	public WorkspaceResponse createWorkspace(
		CreateWorkspaceRequest request,
		Long memberId
	) {
		Member member = memberFinder.findMemberById(memberId);

		// TODO: 오로지 초대 또는 임시 링크를 통한 참여만 가능하도록 변경할 예정.
		//  변경 후에는 워크스페이스의 비밀번호 설정 관련 부분은 삭제해도 될 듯.
		String encodedPassword = Optional.ofNullable(request.password())
			.map(passwordEncoder::encode)
			.orElse(null);

		String workspaceKey = WorkspaceKeyGenerator.generateWorkspaceKey();
		// TODO: 추가 필요
		// workspaceValidator.ensureKeyIsUnique(workspaceKey);

		// TODO: 지금은 "ISSUE"라는 이슈키 접두사를 기본으로 설정하도록 하고 있는데,
		//  이건 추후에 Project 애그리거트 개발을 완료하면, 해당 Project의 key를 issueKeyPrefix로 사용할거임.
		Workspace workspace = Workspace.create(
			workspaceKey,
			request.name(),
			request.description(),
			encodedPassword,
			"ISSUE",
			member
		);

		// TODO: saveAndFlush가 꼭 필요한가?
		//  내 기억상에는 일부러 flush를 통해 key의 유일성 검사(DB 유일성 제약을 통해)를 유도하려고 했던 것 같음.
		Workspace savedWorkspace = workspaceRepository.saveAndFlush(workspace);

		return WorkspaceResponse.from(savedWorkspace);
	}

	@Recover
	public WorkspaceResponse recover(
		DataIntegrityViolationException exception,
		CreateWorkspaceRequest request,
		Long memberId
	) {
		log.error("Retry failed. Workspace code collision could not be resolved after {} attempts.", MAX_RETRIES);
		// TODO: WorkspaceKeyCollisionException extends InternalServerException
		throw new RuntimeException(
			"Failed to solve workspace code collision after %d attempts.".formatted(MAX_RETRIES),
			exception
		);
	}
}
