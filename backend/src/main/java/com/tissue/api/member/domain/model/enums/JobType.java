package com.tissue.api.member.domain.model.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum JobType {
	DEVELOPER("개발자"),
	DESIGNER("디자이너"),
	MARKETER("마케터"),
	MANAGER("매니저"),
	SALES("영업"),
	STUDENT("학생"),
	ETC("기타");

	private final String description;
}
