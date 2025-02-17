package com.tissue.api.workspace.validator;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.AuthenticationFailedException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.security.PasswordEncoder;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceValidator {

	private static final Set<String> RESERVED_PREFIXES = Set.of("SPRINT", "WORKSPACE");

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

	public void validateIssueKeyPrefix(String issueKeyPrefix) {
		if (issueKeyPrefix == null) {
			return;
		}

		String upperIssueKeyPrefix = issueKeyPrefix.toUpperCase();

		if (RESERVED_PREFIXES.contains(upperIssueKeyPrefix)) {
			throw new InvalidOperationException(
				String.format(
					"Issue key prefix cannot be '%s'. Reserved prefixes are: %s",
					issueKeyPrefix,
					String.join(", ", RESERVED_PREFIXES)
				)
			);
		}
	}
}
