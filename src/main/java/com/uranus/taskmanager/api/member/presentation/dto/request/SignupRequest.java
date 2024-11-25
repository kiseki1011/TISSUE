package com.uranus.taskmanager.api.member.presentation.dto.request;

import com.uranus.taskmanager.api.member.domain.Member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
public class SignupRequest {
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

	@Builder
	public SignupRequest(String loginId, String email, String password) {
		this.loginId = loginId;
		this.email = email;
		this.password = password;
	}

	public static Member to(SignupRequest request, String encodedPassword) {
		return Member.builder()
			.loginId(request.loginId)
			.email(request.getEmail())
			.password(encodedPassword)
			.build();
	}
}
