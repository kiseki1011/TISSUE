package com.tissue.member.domain;

import java.util.ArrayList;
import java.util.List;

import com.tissue.common.entity.BaseDateEntity;
import com.tissue.security.authorization.SystemRole;
import com.tissue.workspace.domain.Invitation;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseDateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	// TODO: should i make a Email vo?
	@Column(name = "email", unique = true, nullable = false)
	private String email;

	@Column(name = "username", unique = true, nullable = false)
	private String username;

	@Column(name = "password", nullable = false)
	private String password;

	@Column(name = "name", nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private MemberStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "system_role", nullable = false)
	private SystemRole role;

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Invitation> invitations = new ArrayList<>();

	public static Member create(
		@NonNull String email,
		@NonNull String username,
		@NonNull String password,
		@NonNull String name
	) {
		Member member = new Member();
		member.email = email;
		member.username = username;
		member.password = password;
		member.name = name;
		member.status = MemberStatus.ACTIVE;
		member.role = SystemRole.USER;

		return member;
	}

	public void updateEmail(@NonNull String email) {
		this.email = email;
	}

	public void updateUsername(@NonNull String username) {
		this.username = username;
	}

	public void updatePassword(@NonNull String password) {
		this.password = password;
	}

	public void updateName(@NonNull String name) {
		this.name = name;
	}

	public void active() {
		this.status = MemberStatus.ACTIVE;
	}

	public void withdraw() {
		this.status = MemberStatus.DELETED;
	}
}
