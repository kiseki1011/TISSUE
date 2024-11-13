package com.uranus.taskmanager.api.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateAuthRequest {
	private String password;
}
