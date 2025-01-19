package com.tissue.api.workspace.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.AuthenticationFailedException;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceValidator {

	private final WorkspaceRepository workspaceRepository;
	private final PasswordEncoder passwordEncoder;

	public void validateWorkspacePassword(String inputPassword, String workspaceCode) {
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(() -> new WorkspaceNotFoundException(workspaceCode));

		if (workspace.getPassword() == null) {
			return;
		}
		if (inputPassword == null) {
			throw new AuthenticationFailedException("Workspace password is invalid.");
		}
		if (!passwordEncoder.matches(inputPassword, workspace.getPassword())) {
			throw new AuthenticationFailedException("Workspace password is invalid.");
		}
	}
}
