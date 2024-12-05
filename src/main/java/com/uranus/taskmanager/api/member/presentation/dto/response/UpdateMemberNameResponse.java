package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UpdateMemberNameResponse {
	private Long memberId;
	private LocalDateTime updatedAt;

	@Builder
	public UpdateMemberNameResponse(Long memberId, LocalDateTime updatedAt) {
		this.memberId = memberId;
		this.updatedAt = updatedAt;
	}

	public static UpdateMemberNameResponse from(Member member) {
		return UpdateMemberNameResponse.builder()
			.memberId(member.getId())
			.updatedAt(member.getLastModifiedDate())
			.build();
	}
}
