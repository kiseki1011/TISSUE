package com.tissue.api.workspace.application.service.command;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.application.service.command.MemberFinder;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspace.infrastructure.repository.WorkspaceRepository;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService {

	private final MemberFinder memberFinder;
	private final WorkspaceFinder workspaceFinder;
	private final WorkspaceRepository workspaceRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public WorkspaceResponse updateWorkspaceInfo(
		UpdateWorkspaceInfoRequest request,
		String workspaceCode
	) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceCode);

		updateWorkspaceInfoIfPresent(request, workspace);

		return WorkspaceResponse.from(workspace);
	}

	@Transactional
	public WorkspaceResponse updateWorkspacePassword(
		UpdateWorkspacePasswordRequest request,
		String workspaceCode
	) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceCode);

		String encodedUpdatePassword = encodePasswordIfPresent(request.newPassword());
		workspace.updatePassword(encodedUpdatePassword);

		return WorkspaceResponse.from(workspace);
	}

	/**
	 * Todo
	 *  - hard delete 사용하지 않기(현재 서비스 메서드와 API 제거)
	 *  - WorkspaceStatus를 두고 soft-delete 유도
	 *  - WorkspaceStatus: ACTIVE, DELETED
	 *  - 추후에 Member의 myWorkspaceCount 로직 변경이 필요
	 *  - 워크스페이스 복구 로직 필요
	 *  - 30일 이상 DELETED 상태인 워크스페이스는 배치(batch)로 삭제
	 *  - soft delete으로 변경 후 응답으로 WorkspaceResponse 사용
	 */
	@Transactional
	public void deleteWorkspace(
		String workspaceCode,
		Long memberId
	) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceCode);

		Member member = memberFinder.findMemberById(memberId);
		// member.decreaseMyWorkspaceCount();

		workspaceRepository.delete(workspace);
	}

	@Transactional
	public WorkspaceResponse updateIssueKeyPrefix(
		String workspaceCode,
		UpdateIssueKeyRequest request
	) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceCode);

		workspace.updateIssueKeyPrefix(request.issueKeyPrefix());

		return WorkspaceResponse.from(workspace);
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
