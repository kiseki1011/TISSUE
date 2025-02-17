package com.tissue.api.workspace.service.command;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.member.service.query.MemberQueryService;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.tissue.api.workspace.presentation.dto.response.DeleteWorkspaceResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateIssueKeyResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService {

	private final MemberQueryService memberQueryService;
	private final WorkspaceQueryService workspaceQueryService;
	private final WorkspaceRepository workspaceRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public UpdateWorkspaceInfoResponse updateWorkspaceInfo(
		UpdateWorkspaceInfoRequest request,
		String workspaceCode
	) {
		Workspace workspace = workspaceQueryService.findWorkspace(workspaceCode);

		updateWorkspaceInfoIfPresent(request, workspace);

		return UpdateWorkspaceInfoResponse.from(workspace);
	}

	@Transactional
	public void updateWorkspacePassword(
		UpdateWorkspacePasswordRequest request,
		String workspaceCode
	) {
		Workspace workspace = workspaceQueryService.findWorkspace(workspaceCode);

		String encodedUpdatePassword = encodePasswordIfPresent(request.newPassword());
		workspace.updatePassword(encodedUpdatePassword);
	}

	@Transactional
	public DeleteWorkspaceResponse deleteWorkspace(
		String workspaceCode,
		Long memberId
	) {
		Workspace workspace = workspaceQueryService.findWorkspace(workspaceCode);

		Member member = memberQueryService.findMember(memberId);
		member.decreaseMyWorkspaceCount();

		workspaceRepository.delete(workspace);

		return DeleteWorkspaceResponse.from(workspace);
	}

	@Transactional
	public UpdateIssueKeyResponse updateIssueKeyPrefix(
		String workspaceCode,
		UpdateIssueKeyRequest request
	) {
		Workspace workspace = workspaceQueryService.findWorkspace(workspaceCode);

		workspace.updateIssueKeyPrefix(request.issueKeyPrefix());

		return UpdateIssueKeyResponse.from(workspace);
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
