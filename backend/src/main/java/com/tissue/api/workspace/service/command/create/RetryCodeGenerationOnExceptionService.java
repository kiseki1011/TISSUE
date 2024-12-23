package com.tissue.api.workspace.service.command.create;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.util.WorkspaceCodeGenerator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceCodeCollisionHandleException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DB에서 발생하는 예외를 서비스 계층에서 잡아서 핸들링 로직(워크스페이스 코드 재생성) 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryCodeGenerationOnExceptionService implements WorkspaceCreateService {
	private static final int MAX_RETRIES = 5;

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public CreateWorkspaceResponse createWorkspace(CreateWorkspaceRequest request, Long memberId) {

		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				setWorkspaceCode(request);
				setEncodedPasswordIfPresent(request);

				/*
				 * 테스트 용이성을 위해서 saveWithNewTransaction의 사용을 고려하자
				 */
				Workspace workspace = workspaceRepository.saveAndFlush(CreateWorkspaceRequest.to(request));
				addOwnerMemberToWorkspace(member, workspace);

				return CreateWorkspaceResponse.from(workspace);
			} catch (DataIntegrityViolationException | ConstraintViolationException e) {
				log.error("Catched exception for workspace code collision.");
				log.error("Exception: ", e);
				log.info("Workspace code collision occured. Retry attempt: #{}", attempt);
			}
		}
		throw new WorkspaceCodeCollisionHandleException();
	}

	private void addOwnerMemberToWorkspace(Member member, Workspace workspace) {
		WorkspaceMember workspaceMember = WorkspaceMember.addOwnerWorkspaceMember(member, workspace);
		workspaceMemberRepository.save(workspaceMember);
	}

	private void setWorkspaceCode(CreateWorkspaceRequest request) {
		String generatedCode = workspaceCodeGenerator.generateWorkspaceCode();
		request.setCode(generatedCode);
	}

	private void setEncodedPasswordIfPresent(CreateWorkspaceRequest request) {
		String encodedPassword = Optional.ofNullable(request.getPassword())
			.map(passwordEncoder::encode)
			.orElse(null);
		request.setPassword(encodedPassword);
	}
}
