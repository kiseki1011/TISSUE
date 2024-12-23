package com.tissue.api.member.presentation.dto.request;

import java.time.LocalDate;

import com.tissue.api.member.domain.JobType;
import com.tissue.api.member.domain.Member;
import com.tissue.api.member.domain.vo.Name;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignupMemberRequest {
	/**
	 * Todo
	 *  - 방법1: 재사용 될 수 있는 패턴 검증은 Validator 클래스로 분리, 서비스에서 검증
	 *  - 방법2: 커스텀 검증 애노테이션을 구현해서 적용(재사용성 증가)
	 *  - 방법3: 지금 처럼 그냥 제공 애노테이션으로 필드에 대한 기본 검증 진행
	 *  - 매직 넘버는 상수로 만들기
	 */
	@NotBlank(message = "Login ID must not be blank")
	@Pattern(regexp = "^[a-zA-Z0-9]{2,20}$",
		message = "Login ID must be alphanumeric"
			+ " and must be between 2 and 20 characters")
	private String loginId;

	@NotBlank(message = "Email must not be blank")
	@Size(min = 5, max = 254, message = "Email must be between 5 and 254 characters")
	@Email(message = "Email should be in a valid format")
	private String email;

	@NotBlank(message = "Password must not be blank")
	@Pattern(regexp = "^(?!.*[가-힣])(?=.*[0-9])(?=.*[a-zA-Z])(?=.*\\W)(?=\\S+$).{8,30}",
		message = "The password must be alphanumeric"
			+ " including at least one special character and must be between 8 and 30 characters")
	private String password;

	@NotBlank(message = "First name must not be blank")
	@Size(max = 50, message = "First name must be less than 50 characters")
	private String firstName;

	@NotBlank(message = "Last name must not be blank")
	@Size(max = 50, message = "Last name must be less than 50 characters")
	private String lastName;

	@NotNull(message = "Birth date must not be null")
	@Past(message = "Birth date must be in the past")
	private LocalDate birthDate;

	@NotNull(message = "Job type must not be null")
	@Enumerated(EnumType.STRING)
	private JobType jobType;

	/**
	 * Todo
	 *  - introduction에 size 검증 필요
	 */
	@Size(max = 255, message = "Introduction must be less than 255 characters")
	private String introduction;

	@Builder
	public SignupMemberRequest(
		String loginId,
		String email,
		String password,
		String firstName,
		String lastName,
		LocalDate birthDate,
		JobType jobType,
		String introduction
	) {
		this.loginId = loginId;
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.birthDate = birthDate;
		this.jobType = jobType;
		this.introduction = introduction;
	}

	public Member toEntity(String encodedPassword) {
		return Member.builder()
			.loginId(this.loginId)
			.email(this.email)
			.password(encodedPassword)
			.name(Name.builder()
				.firstName(this.firstName)
				.lastName(this.lastName)
				.build())
			.birthDate(this.birthDate)
			.jobType(this.jobType)
			.introduction(this.introduction)
			.build();
	}
}
