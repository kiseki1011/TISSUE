package com.uranus.taskmanager.api.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class InviteMemberRequest {

	@NotBlank(message = "Member identifier must not be blank")
	private String memberIdentifier;

	public InviteMemberRequest(String memberIdentifier) {
		this.memberIdentifier = memberIdentifier;
	}
}
