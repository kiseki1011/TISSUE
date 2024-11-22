package com.uranus.taskmanager.api.member.domain;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.common.entity.BaseDateEntity;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.exception.InvalidWorkspaceCountException;
import com.uranus.taskmanager.api.workspacemember.exception.WorkspaceCreationLimitExceededException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MEMBER_ID")
	Long id;

	@Column(unique = true, nullable = false)
	private String loginId;
	@Column(unique = true, nullable = false)
	private String email;
	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private int myWorkspaceCount = 0;

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	@OneToMany(mappedBy = "member")
	private List<Invitation> invitations = new ArrayList<>();

	@Builder
	public Member(String loginId, String email, String password) {
		this.loginId = loginId;
		this.email = email;
		this.password = password;
	}

	public void increaseMyWorkspaceCount() {
		validateWorkpspaceLimit();
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

	private void validateWorkpspaceLimit() {
		// Todo: 최대 워크스페이스 소유수를 상수로 분리(현재는 50)
		if (this.myWorkspaceCount >= 50) {
			throw new WorkspaceCreationLimitExceededException();
		}
	}

	private void validatePositiveMyWorkspaceCount() {
		if (this.myWorkspaceCount <= 0) {
			throw new InvalidWorkspaceCountException();
		}
	}
}
