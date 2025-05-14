package com.tissue.api.member.domain.model.vo;

import java.util.Locale;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Name {

	private String firstName;
	private String lastName;

	@Builder
	public Name(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	/**
	 * Todo
	 *  - Locale에 따라 다른 FullName을 반환하도록 해야 함
	 *  - 예시: 한국 - "성" + "이름"
	 *  - 예시: 미국 - "이름" + "성"
	 *  - 방법1
	 *    - 방법은 기본적으로 백엔드에서 locale(지역) 정보에 따라 알맞게 조합해주고,
	 *    - 클라이언트단에서 필요한 경우 성과 이름의 순서를 바꾸는게 가능하도록 설정?
	 *  - 방법2(이게 제일 좋을 듯)
	 *    - 입력한 지역 정보에 따라 언어와 전체 이름 조합 방법이 선택되는 방식으로
	 */
	public String getLocalizedFullName(Locale locale) {
		if (locale.equals(Locale.KOREA)) {
			return lastName + " " + firstName;
		} else {
			return firstName + " " + lastName;
		}
	}

}
