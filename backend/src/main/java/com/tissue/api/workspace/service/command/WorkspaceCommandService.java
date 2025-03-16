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
import com.tissue.api.workspace.validator.WorkspaceValidator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService {

	private final MemberQueryService memberQueryService;
	private final WorkspaceReader workspaceReader;
	private final WorkspaceRepository workspaceRepository;
	private final PasswordEncoder passwordEncoder;
	private final WorkspaceValidator workspaceValidator;

	@Transactional
	public UpdateWorkspaceInfoResponse updateWorkspaceInfo(
		UpdateWorkspaceInfoRequest request,
		String workspaceCode
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		updateWorkspaceInfoIfPresent(request, workspace);

		return UpdateWorkspaceInfoResponse.from(workspace);
	}

	@Transactional
	public void updateWorkspacePassword(
		UpdateWorkspacePasswordRequest request,
		String workspaceCode
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		String encodedUpdatePassword = encodePasswordIfPresent(request.newPassword());
		workspace.updatePassword(encodedUpdatePassword);
	}

	/**
	 * Todo
	 *  - hard delete 사용하지 않기(현재 서비스 메서드와 API 제거)
	 *  - WorkspaceStatus를 두고 soft-delete 유도
	 *  - WorkspaceStatus: ACTIVE, DELETED
	 *  - 추후에 Member의 myWorkspaceCount 로직 변경이 필요
	 *  - 워크스페이스 복구 로직 필요
	 *  - 30일 이상 DELETED 상태인 워크스페이스는 배치(batch)로 삭제
	 */
	@Transactional
	public DeleteWorkspaceResponse deleteWorkspace(
		String workspaceCode,
		Long memberId
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

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
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		workspaceValidator.validateIssueKeyPrefix(request.issueKeyPrefix());
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
