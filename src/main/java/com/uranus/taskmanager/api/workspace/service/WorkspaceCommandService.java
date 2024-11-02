package com.uranus.taskmanager.api.workspace.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceUpdateDetail;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceContentUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspacePasswordUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceContentUpdateResponse;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WorkspaceCommandService {

	private final WorkspaceRepository workspaceRepository;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public WorkspaceContentUpdateResponse updateWorkspaceContent(WorkspaceContentUpdateRequest request, String code) {
		Workspace workspace = findWorkspaceByCode(code);
		WorkspaceUpdateDetail original = WorkspaceUpdateDetail.from(workspace);

		if (request.hasName()) {
			workspace.updateName(request.getName());
		}
		if (request.hasDescription()) {
			workspace.updateDescription(request.getDescription());
		}

		WorkspaceUpdateDetail updatedTo = WorkspaceUpdateDetail.from(workspace);
		return WorkspaceContentUpdateResponse.from(original, updatedTo);
	}

	@Transactional
	public void updateWorkspacePassword(WorkspacePasswordUpdateRequest request, String code) {
		Workspace workspace = findWorkspaceByCode(code);

		validatePasswordIfExists(workspace.getPassword(), request.getOriginalPassword());

		String encodedUpdatePassword = encodePasswordIfPresent(request.getUpdatePassword());
		workspace.updatePassword(encodedUpdatePassword);
	}

	@Transactional
	public void deleteWorkspace(WorkspaceDeleteRequest request, String code) {
		Workspace workspace = findWorkspaceByCode(code);

		validatePasswordIfExists(workspace.getPassword(), request.getPassword());

		workspaceRepository.delete(workspace);
	}

	private Workspace findWorkspaceByCode(String workspaceCode) {
		return workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);
	}

	private String encodePasswordIfPresent(String password) {
		return Optional.ofNullable(password)
			.map(passwordEncoder::encode)
			.orElse(null);
	}

	private void validatePasswordIfExists(String workspacePassword, String inputPassword) {
		if (workspacePassword == null) {
			return;
		}
		if (passwordDoesNotMatch(workspacePassword, inputPassword)) {
			throw new InvalidWorkspacePasswordException();
		}
	}

	private boolean passwordDoesNotMatch(String workspacePassword, String inputPassword) {
		return !passwordEncoder.matches(inputPassword, workspacePassword);
	}
}
