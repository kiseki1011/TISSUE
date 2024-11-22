package com.uranus.taskmanager.api.workspace.validator;

import org.springframework.stereotype.Component;

import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceValidator {

	private final WorkspaceRepository workspaceRepository;

	private final PasswordEncoder passwordEncoder;

	public boolean validateWorkspaceCodeIsUnique(String code) {
		return !workspaceRepository.existsByCode(code);
	}

	public void validatePasswordIfExists(String workspacePassword, String inputPassword) {
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
