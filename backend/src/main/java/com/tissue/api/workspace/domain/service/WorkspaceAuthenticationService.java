package com.tissue.api.workspace.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.command.WorkspaceReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceAuthenticationService {

	private final WorkspaceReader workspaceReader;
	private final PasswordEncoder passwordEncoder;

	public void authenticate(String rawPassword, String workspaceCode) {

		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

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
