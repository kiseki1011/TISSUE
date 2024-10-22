package com.uranus.taskmanager.api.workspacemember;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceRole {
	ADMIN(3),
	USER(2),
	READER(1);

	private final int level;
}
