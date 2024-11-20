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
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
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

		Member member = findMemberById(memberId);

		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				setWorkspaceCode(request);
				setEncodedPasswordIfPresent(request);

				Workspace workspace = workspaceRepository.saveWithNewTransaction(request.to());
				addOwnerMemberToWorkspace(member, workspace);

				return WorkspaceCreateResponse.from(workspace);
			} catch (DataIntegrityViolationException | ConstraintViolationException e) {
				log.error("[Catched Exception for Workspace Code Collision]", e);
				log.info("[Workspace Code Collision] Retrying... attempt {}", attempt);
			}
		}
		throw new WorkspaceCodeCollisionHandleException();
	}

	private void addOwnerMemberToWorkspace2(Member member, Workspace workspace) {
		WorkspaceMember workspaceMember = WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.OWNER,
			member.getEmail());

		workspaceMemberRepository.save(workspaceMember);
	}

	private void addOwnerMemberToWorkspace(Member member, Workspace workspace) {
		WorkspaceMember workspaceMember = WorkspaceMember.addOwnerWorkspaceMember(member, workspace);

		workspaceMemberRepository.save(workspaceMember);
	}

	private void setWorkspaceCode(WorkspaceCreateRequest workspaceCreateRequest) {
		String code = workspaceCodeGenerator.generateWorkspaceCode();
		workspaceCreateRequest.setCode(code);
	}

	private void setEncodedPasswordIfPresent(WorkspaceCreateRequest workspaceCreateRequest) {
		String encodedPassword = encodePasswordIfPresent(workspaceCreateRequest.getPassword());
		workspaceCreateRequest.setPassword(encodedPassword);
	}

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(MemberNotFoundException::new);
	}

	private String encodePasswordIfPresent(String password) {
		return Optional.ofNullable(password)
			.map(passwordEncoder::encode)
			.orElse(null);
	}
}
