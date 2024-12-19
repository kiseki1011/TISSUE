package com.uranus.taskmanager.api.member.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.common.entity.BaseDateEntity;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.member.domain.vo.Name;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.InvalidWorkspaceCountException;
import com.uranus.taskmanager.api.workspacemember.exception.WorkspaceCreationLimitExceededException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseDateEntity {

	/**
	 * Todo
	 *  - 상수는 원래 @ConfigurationProperties를 통해 관리하려고 했음
	 *  - 그러나 엔티티의 상수를 위해 스프링 의존성을 사용하고 싶지는 않음
	 *  - 그래서 결국 엔티티에 정의해서 사용
	 *  - 더 좋은 설계가 있는지 한번 고민 필요
	 */
	private static final int MAX_MY_WORKSPACE_COUNT = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MEMBER_ID")
	private Long id;

	@Column(unique = true, nullable = false)
	private String loginId;
	@Column(unique = true, nullable = false)
	private String email;
	@Column(nullable = false)
	private String password;

	@Embedded
	@Column(nullable = false)
	private Name name;

	@Lob
	private String introduction;

	@Column(nullable = false)
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobType jobType;

	@Column(nullable = false)
	private int myWorkspaceCount = 0;

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Invitation> invitations = new ArrayList<>();

	@Builder
	public Member(
		String loginId,
		String email,
		String password,
		String introduction,
		JobType jobType,
		Name name,
		LocalDate birthDate
	) {
		this.loginId = loginId;
		this.email = email;
		this.password = password;
		this.introduction = introduction;
		this.jobType = jobType;
		this.name = name;
		this.birthDate = birthDate;
	}

	public void increaseMyWorkspaceCount() {
		validateWorkspaceLimit();
		this.myWorkspaceCount++;
	}

	public void decreaseMyWorkspaceCount() {
		validatePositiveMyWorkspaceCount();
		this.myWorkspaceCount--;
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	public void updateEmail(String email) {
		this.email = email;
	}

	public void updateName(Name name) {
		this.name = name;
	}

	public void updateIntroduction(String introduction) {
		this.introduction = introduction;
	}

	public void updateBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public void updateJobType(JobType jobType) {
		this.jobType = jobType;
	}

	private void validateWorkspaceLimit() {
		if (this.myWorkspaceCount >= MAX_MY_WORKSPACE_COUNT) {
			throw new WorkspaceCreationLimitExceededException();
		}
	}

	private void validatePositiveMyWorkspaceCount() {
		if (this.myWorkspaceCount <= 0) {
			throw new InvalidWorkspaceCountException();
		}
	}
}
