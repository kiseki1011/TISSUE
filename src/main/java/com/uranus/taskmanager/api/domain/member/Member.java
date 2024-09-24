package com.uranus.taskmanager.api.domain.member;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.domain.workspaceuser.WorkspaceMember;

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
public class Member {
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

	@OneToMany(mappedBy = "member")
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();
	
	@Builder
	public Member(String loginId, String email, String password) {
		this.loginId = loginId;
		this.email = email;
		this.password = password;
	}
}
