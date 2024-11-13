package com.uranus.taskmanager.api.member.domain;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.common.entity.BaseDateEntity;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.member.exception.WorkspaceCreationLimitExceededException;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

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

	/**
	 * Todo
	 * 	- 이슈 #82 참고
	 * 	- 워크스페이스 생성/유지 한도를 관리하기 위해 사용(최대 50개)
	 * 	- 정보 제공 목적으로 사용
	 * 	- 추후에 50을 상수로 따로 분리, 외부 설정으로 설정을 주입할 수 있도록 구현해야 함
	 */
	@Column(nullable = false)
	private int workspaceCount = 0;

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

	public void increaseWorkspaceCount() {
		if (this.workspaceCount >= 50) {
			throw new WorkspaceCreationLimitExceededException();
		}
		this.workspaceCount++;
	}

	public void decreaseWorkspaceCount() {
		if (this.workspaceCount > 0) {
			this.workspaceCount--;
		}
	}

	public void updatePassword(String password) {
		this.password = password;
	}

	public void updateEmail(String email) {
		this.email = email;
	}
}
