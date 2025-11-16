package com.tissue.api.member.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseDateEntity;
import com.tissue.api.invitation.domain.model.Invitation;
import com.tissue.api.security.authorization.enums.SystemRole;

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

	// TODO: Email VO를 만들어서 사용?
	@Column(unique = true, nullable = false)
	private String email;

	// TODO: Username VO를 만들어서 사용?
	@Column(unique = true, nullable = false)
	private String username;

	@Column(nullable = false)
	private String password;

	// TODO: name, birthDate MemberProfile라는 VO로 묶기?
	//  아니면 Profile VO는 만들지 않고 Name VO를 만들어서 사용하기?
	private String name;

	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	private SystemRole role;

	@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Invitation> invitations = new ArrayList<>();

	public static Member create(
		@NonNull String email,
		@NonNull String username,
		@NonNull String password,
		@Nullable String name,
		@Nullable LocalDate birthDate
	) {
		Member member = new Member();
		member.email = email;
		member.username = username;
		member.password = password;
		member.name = name;
		member.birthDate = birthDate;
		member.role = SystemRole.USER;

		return member;
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
}
