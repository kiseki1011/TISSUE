package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UpdateMemberEmailResponse {

	private Long memberId;
	private LocalDateTime updatedAt;

	@Builder
	public UpdateMemberEmailResponse(Long memberId, LocalDateTime updatedAt) {
		this.memberId = memberId;
		this.updatedAt = updatedAt;
	}

	public static UpdateMemberEmailResponse from(Member member) {
		return UpdateMemberEmailResponse.builder()
			.memberId(member.getId())
			.updatedAt(member.getLastModifiedDate())
			.build();
	}
}
