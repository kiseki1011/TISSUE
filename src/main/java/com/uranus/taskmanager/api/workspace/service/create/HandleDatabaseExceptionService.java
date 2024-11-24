package com.uranus.taskmanager.api.workspace.service.create;

import java.util.Optional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.util.WorkspaceCodeGenerator;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceCodeCollisionHandleException;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.WorkspaceCreateResponse;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DB에서 발생하는 예외를 서비스 계층에서 잡아서 핸들링 로직(워크스페이스 코드 재생성) 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandleDatabaseExceptionService implements WorkspaceCreateService {
	private static final int MAX_RETRIES = 5;

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceCodeGenerator workspaceCodeGenerator;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public WorkspaceCreateResponse createWorkspace(WorkspaceCreateRequest request, Long memberId) {

		Member member = memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);

		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				setWorkspaceCode(request);
				setEncodedPasswordIfPresent(request);

				Workspace workspace = workspaceRepository.saveWithNewTransaction(request.to());
				addOwnerMemberToWorkspace(member, workspace);

				return WorkspaceCreateResponse.from(workspace);
			} catch (DataIntegrityViolationException | ConstraintViolationException e) {
				log.error("Catched Exception for Workspace Code Collision: ", e);
				log.info("[Workspace Code Collision] Retry Attempt #{}", attempt);
			}
		}
		throw new WorkspaceCodeCollisionHandleException();
	}

	private void addOwnerMemberToWorkspace(Member member, Workspace workspace) {
		WorkspaceMember workspaceMember = WorkspaceMember.addOwnerWorkspaceMember(member, workspace);
		workspaceMemberRepository.save(workspaceMember);
	}

	private void setWorkspaceCode(WorkspaceCreateRequest request) {
		String generatedCode = workspaceCodeGenerator.generateWorkspaceCode();
		request.setCode(generatedCode);
	}

	private void setEncodedPasswordIfPresent(WorkspaceCreateRequest request) {
		String encodedPassword = Optional.ofNullable(request.getPassword())
			.map(passwordEncoder::encode)
			.orElse(null);
		request.setPassword(encodedPassword);
	}
}
