package com.uranus.taskmanager.api.member.presentation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.uranus.taskmanager.api.member.domain.JobType;
import com.uranus.taskmanager.api.member.domain.Member;

import lombok.Builder;
import lombok.Getter;

@Getter
public class MemberDetail {

	private String loginId;
	private String email;
	private String lastName;
	private String firstName;
	private LocalDate birthDate;
	private JobType jobType;
	private String introduction;
	private int ownedWorkspaceCount;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Builder
	public MemberDetail(
		String loginId,
		String email,
		String lastName,
		String firstName,
		LocalDate birthDate,
		JobType jobType,
		String introduction,
		int ownedWorkspaceCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
	) {
		this.loginId = loginId;
		this.email = email;
		this.lastName = lastName;
		this.firstName = firstName;
		this.birthDate = birthDate;
		this.jobType = jobType;
		this.introduction = introduction;
		this.ownedWorkspaceCount = ownedWorkspaceCount;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static MemberDetail from(Member member) {
		return MemberDetail.builder()
			.loginId(member.getLoginId())
			.email(member.getEmail())
			.lastName(member.getName().getLastName())
			.firstName(member.getName().getFirstName())
			.birthDate(member.getBirthDate())
			.jobType(member.getJobType())
			.introduction(member.getIntroduction())
			.ownedWorkspaceCount(member.getMyWorkspaceCount())
			.createdAt(member.getCreatedDate())
			.updatedAt(member.getLastModifiedDate())
			.build();
	}
}
