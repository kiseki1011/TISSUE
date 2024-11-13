package com.uranus.taskmanager.api.member.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateAuthRequest {
	private String password;

	public UpdateAuthRequest(String password) {
		this.password = password;
	}
}
