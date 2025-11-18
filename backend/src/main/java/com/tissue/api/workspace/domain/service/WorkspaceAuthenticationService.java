package com.tissue.api.workspace.domain.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tissue.api.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.api.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceAuthenticationService {

	private final WorkspaceFinder workspaceFinder;
	private final PasswordEncoder passwordEncoder;

	// TODO: 어차피 Workspace 비밀번호 기능은 제거할 생각임. 오로지 임시 링크 또는 초대로만 참여 가능하도록.
	public void authenticate(String rawPassword, String workspaceCode) {

		Workspace workspace = workspaceFinder.findWorkspace(workspaceCode);

		if (workspace.getPassword() == null) {
			return;
		}
		if (rawPassword == null) {
			// TODO: SprintSecurity의 AuthenticationExcpetion 활용? or 커스텀 예외 만들어서 사용?
			throw new RuntimeException("Workspace password is invalid.");
		}
		if (!StringUtils.hasText(rawPassword) || !passwordEncoder.matches(rawPassword, workspace.getPassword())) {
			// TODO: SprintSecurity의 AuthenticationExcpetion 활용? or 커스텀 예외 만들어서 사용?
			throw new RuntimeException("Invalid workspace password.");
		}
	}

}
