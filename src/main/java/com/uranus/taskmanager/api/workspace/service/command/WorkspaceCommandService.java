package com.uranus.taskmanager.api.workspace.service.command;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.domain.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.DeleteWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.DeleteWorkspaceResponse;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;
import com.uranus.taskmanager.api.workspace.validator.WorkspaceValidator;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final WorkspaceValidator workspaceValidator;

	@Transactional
	public UpdateWorkspaceInfoResponse updateWorkspaceInfo(
		UpdateWorkspaceInfoRequest request,
		String code
	) {
		Workspace workspace = findWorkspaceByCode(code);

		updateWorkspaceInfoIfPresent(request, workspace);

		return UpdateWorkspaceInfoResponse.from(workspace);
	}

	@Transactional
	public void updateWorkspacePassword(
		UpdateWorkspacePasswordRequest request,
		String code
	) {
		Workspace workspace = findWorkspaceByCode(code);

		workspaceValidator.validatePasswordIfExists(workspace.getPassword(), request.getOriginalPassword());

		String encodedUpdatePassword = encodePasswordIfPresent(request.getUpdatePassword());
		workspace.updatePassword(encodedUpdatePassword);
	}

	@Transactional
	public DeleteWorkspaceResponse deleteWorkspace(
		DeleteWorkspaceRequest request,
		String code,
		Long memberId
	) {
		Workspace workspace = findWorkspaceByCode(code);

		Member member = findMemberById(memberId);
		member.decreaseMyWorkspaceCount();

		workspaceValidator.validatePasswordIfExists(workspace.getPassword(), request.getPassword());
		workspaceRepository.delete(workspace);

		return DeleteWorkspaceResponse.from(code);
	}

	private Workspace findWorkspaceByCode(String code) {
		return workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);
	}

	private Member findMemberById(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(MemberNotInWorkspaceException::new);
	}

	private void updateWorkspaceInfoIfPresent(UpdateWorkspaceInfoRequest request, Workspace workspace) {
		if (request.hasName()) {
			workspace.updateName(request.getName());
		}
		if (request.hasDescription()) {
			workspace.updateDescription(request.getDescription());
		}
	}

	private String encodePasswordIfPresent(String password) {
		return Optional.ofNullable(password)
			.map(passwordEncoder::encode)
			.orElse(null);
	}
}
