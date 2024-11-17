package com.uranus.taskmanager.api.member.presentation.dto;

import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class MemberDetail {
	private Long id;
	private String loginId;
	private String email;
	private LocalDateTime joinedAt;
	private LocalDateTime updatedAt;
	private int createdWorkspaceCount;

	@Builder
	public MemberDetail(Long id, String loginId, String email, LocalDateTime joinedAt, LocalDateTime updatedAt,
		int createdWorkspaceCount) {
		this.id = id;
		this.loginId = loginId;
		this.email = email;
		this.joinedAt = joinedAt;
		this.updatedAt = updatedAt;
		this.createdWorkspaceCount = createdWorkspaceCount;
	}

	public static MemberDetail from(Member member) {
		return MemberDetail.builder()
			.id(member.getId())
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.joinedAt(member.getCreatedDate())
			.updatedAt(member.getLastModifiedDate())
			.createdWorkspaceCount(member.getWorkspaceCount())
			.build();
	}
}
