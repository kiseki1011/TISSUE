package com.tissue.api.workspace.domain.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceAuthenticationService {

	private final WorkspaceFinder workspaceFinder;
	private final PasswordEncoder passwordEncoder;

	public void authenticate(String rawPassword, String workspaceCode) {

		Workspace workspace = workspaceFinder.findWorkspace(workspaceCode);

		if (workspace.getPassword() == null) {
			return;
		}
		if (rawPassword == null) {
			throw new AuthenticationFailedException("Workspace password is invalid.");
		}
		if (!StringUtils.hasText(rawPassword) || !passwordEncoder.matches(rawPassword, workspace.getPassword())) {
			throw new AuthenticationFailedException("Invalid workspace password.");
		}
	}

}
