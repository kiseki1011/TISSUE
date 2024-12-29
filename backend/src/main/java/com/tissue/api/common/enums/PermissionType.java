package com.tissue.api.common.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum PermissionType {
	MEMBER_UPDATE,
	MEMBER_DELETE,
	WORKSPACE_JOIN,
	WORKSPACE_PASSWORD_UPDATE,
	WORKSPACE_DELETE
}
