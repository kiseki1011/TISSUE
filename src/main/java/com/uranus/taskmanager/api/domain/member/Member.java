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
	private String userId;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

	@OneToMany(mappedBy = "member")
	private List<WorkspaceMember> workspaceMembers = new ArrayList<>();

	/**
	 * Todo
	 * @param id - 빌더에 id 필드 제외를 고려하자(테스트를 위해 리플렉션을 사용)
	 */
	@Builder
	public Member(Long id, String userId, String email, String password) {
		this.id = id;
		this.userId = userId;
		this.email = email;
		this.password = password;
	}
}
