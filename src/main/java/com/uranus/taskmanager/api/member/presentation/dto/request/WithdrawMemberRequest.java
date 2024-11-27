package com.uranus.taskmanager.api.member.presentation.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawMemberRequest {

	private String password;

	public WithdrawMemberRequest(String password) {
		this.password = password;
	}
}
