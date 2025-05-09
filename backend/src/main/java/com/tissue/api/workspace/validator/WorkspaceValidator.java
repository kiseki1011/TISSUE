package com.tissue.api.workspace.validator;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceValidator {

	// TODO: Workspace 엔티티로 RESERVED_PREFIXES 를 옮기는거나, 따로 ENUM으로 옮기기?
	private static final Set<String> RESERVED_PREFIXES = Set.of("SPRINT", "WORKSPACE");

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
