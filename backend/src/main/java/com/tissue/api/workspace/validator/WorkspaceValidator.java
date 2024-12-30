package com.tissue.api.workspace.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.InvalidWorkspacePasswordException;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceValidator {

	private final WorkspaceRepository workspaceRepository;
	private final PasswordEncoder passwordEncoder;

	public void validateWorkspacePassword(String inputPassword, String workspaceCode) {
		Workspace workspace = workspaceRepository.findByCode(workspaceCode)
			.orElseThrow(WorkspaceNotFoundException::new);

		if (workspace.getPassword() == null) {
			return;
		}
		if (inputPassword == null) {
			throw new InvalidWorkspacePasswordException();
		}
		if (!passwordEncoder.matches(inputPassword, workspace.getPassword())) {
			throw new InvalidWorkspacePasswordException();
		}
	}

	public boolean validateWorkspaceCodeIsUnique(String code) {
		return !workspaceRepository.existsByCode(code);
	}
}
