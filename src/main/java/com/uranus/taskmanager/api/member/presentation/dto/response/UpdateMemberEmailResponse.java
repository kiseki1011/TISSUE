package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UpdateMemberEmailResponse {

	private Long memberId;
	private LocalDateTime updatedAt;
	private EmailUpdate emailUpdate;

	@Builder
	public UpdateMemberEmailResponse(Long memberId, LocalDateTime updatedAt, EmailUpdate emailUpdate) {
		this.memberId = memberId;
		this.updatedAt = updatedAt;
		this.emailUpdate = emailUpdate;
	}

	@Getter
	public static class EmailUpdate {
		private final String previousEmail;
		private final String updatedEmail;

		private EmailUpdate(String previousEmail, String updatedEmail) {
			this.previousEmail = previousEmail;
			this.updatedEmail = updatedEmail;
		}
	}

	public static UpdateMemberEmailResponse from(Member member, String previousEmail) {
		return UpdateMemberEmailResponse.builder()
			.memberId(member.getId())
			.updatedAt(member.getLastModifiedDate())
			.emailUpdate(new EmailUpdate(previousEmail, member.getEmail()))
			.build();
	}
}
