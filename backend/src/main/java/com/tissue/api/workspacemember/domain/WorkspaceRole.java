package com.tissue.api.workspacemember.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceRole {
	OWNER(5),
	ADMIN(4),
	MANAGER(3),
	COLLABORATOR(2),
	VIEWER(1);

	private final int level;
}
