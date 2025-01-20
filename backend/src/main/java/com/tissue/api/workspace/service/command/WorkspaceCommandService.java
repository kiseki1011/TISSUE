package com.tissue.api.workspace.service.command;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.repository.MemberRepository;
import com.tissue.api.member.exception.MemberNotFoundException;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.tissue.api.workspace.presentation.dto.response.DeleteWorkspaceResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateIssueKeyResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService {

	private final WorkspaceRepository workspaceRepository;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UpdateWorkspaceInfoResponse updateWorkspaceInfo(
		UpdateWorkspaceInfoRequest request,
		String code
	) {
		Workspace workspace = findWorkspace(code);

		updateWorkspaceInfoIfPresent(request, workspace);

		return UpdateWorkspaceInfoResponse.from(workspace);
	}

	@Transactional
	public void updateWorkspacePassword(
		UpdateWorkspacePasswordRequest request,
		String code
	) {
		Workspace workspace = findWorkspace(code);

		String encodedUpdatePassword = encodePasswordIfPresent(request.newPassword());
		workspace.updatePassword(encodedUpdatePassword);
	}

	@Transactional
	public DeleteWorkspaceResponse deleteWorkspace(
		String code,
		Long memberId
	) {
		Workspace workspace = findWorkspace(code);

		Member member = findMember(memberId);
		member.decreaseMyWorkspaceCount();

		workspaceRepository.delete(workspace);

		return DeleteWorkspaceResponse.from(workspace);
	}

	@Transactional
	public UpdateIssueKeyResponse updateIssueKey(
		String code,
		UpdateIssueKeyRequest request
	) {
		Workspace workspace = findWorkspace(code);

		workspace.updateKeyPrefix(request.issueKeyPrefix());

		return UpdateIssueKeyResponse.from(workspace);
	}

	private Workspace findWorkspace(String code) {
		return workspaceRepository.findByCode(code)
			.orElseThrow(() -> new WorkspaceNotFoundException(code));
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
			.orElseThrow(() -> new MemberNotFoundException(memberId));
	}

	private void updateWorkspaceInfoIfPresent(UpdateWorkspaceInfoRequest request, Workspace workspace) {
		if (request.hasName()) {
			workspace.updateName(request.name());
		}
		if (request.hasDescription()) {
			workspace.updateDescription(request.description());
		}
	}

	private String encodePasswordIfPresent(String password) {
		return Optional.ofNullable(password)
			.map(passwordEncoder::encode)
			.orElse(null);
	}
}
