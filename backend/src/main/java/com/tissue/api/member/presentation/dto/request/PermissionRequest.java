package com.tissue.api.member.presentation.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PermissionRequest {

	private String password;

	public PermissionRequest(String password) {
		this.password = password;
	}
}
