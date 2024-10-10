package com.uranus.taskmanager.api.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.ToString;

/**
 * Todo: final을 붙이는 경우 json serialization 문제 발생
 * final을 붙이고, 기본 생성자를 생략해서 사용할 수 있는 방법 찾아보기
 * final을 생략하고 기본 생성자 추가 시 해결
 */
@ToString
@Getter
public class InviteMemberRequest {

	@NotBlank(message = "Member identifier must not be blank")
	// private final String memberIdentifier;
	private String memberIdentifier;

	public InviteMemberRequest() {
	}

	public InviteMemberRequest(String memberIdentifier) {
		this.memberIdentifier = memberIdentifier;
	}
}
