package com.tissue.api.member.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseDateEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.member.domain.model.enums.JobType;
import com.tissue.api.security.authorization.enums.SystemRole;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: soft delete 적용
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseDateEntity {

	// TODO: 애플리케이션 서비스에서 XxxPolicy를 호출하는 형태로 리팩토링(거기서 application.yml에서 설정값을 읽는 방식)
	private static final int MAX_WORKSPACE_COUNT = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Column(unique = true, nullable = false)
	private String loginId;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(unique = true, nullable = false)
	private String username;

	@Column(nullable = false)
	private String password;

	private String name;

	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	private JobType jobType;

	@Enumerated(EnumType.STRING)
	private SystemRole role;

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Invitation> invitations = new ArrayList<>();

	// TODO: 워크스페이스 카운트 검증은 애플리케이션 서비스에서 XxxPolicy를 호출하는 형태로 리팩토링
	// TODO: 생성자(빌더) 사용 대신 정적 팩토리 메서드 사용
	@Builder
	public Member(
		String loginId,
		String email,
		String username,
		String password,
		JobType jobType,
		String name,
		LocalDate birthDate
	) {
		this.loginId = loginId;
		this.email = email;
		this.username = username;
		this.password = password;
		this.jobType = jobType;
		this.name = name;
		this.birthDate = birthDate;
		this.role = SystemRole.USER;
	}

	public int getWorkspaceCount() {
		return workspaceMembers.size();
	}

	public void updateEmail(String email) {
		this.email = email;
	}

	public void updateUsername(String username) {
		this.username = username;
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	public void updateName(String name) {
		this.name = name;
	}

	public void updateBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public void updateJobType(JobType jobType) {
		this.jobType = jobType;
	}

	public void updateRole(SystemRole role) {
		this.role = role;
	}

	public void validateWorkspaceLimit() {
		if (getWorkspaceCount() >= MAX_WORKSPACE_COUNT) {
			throw new InvalidOperationException(
				String.format("Max number of workspaces a member can have is %d.", MAX_WORKSPACE_COUNT));
		}
	}
}
