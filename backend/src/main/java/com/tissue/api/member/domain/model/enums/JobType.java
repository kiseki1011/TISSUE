package com.tissue.api.member.domain.model.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// TODO: description에 message 사용
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum JobType {
	DEVELOPER("개발자"),
	BACKEND_DEVELOPER("백엔드 개발자"),
	FRONTEND_DEVELOPER("프론트엔드 개발자"),
	FULLSTACK_DEVELOPER("풀스택 개발자"),
	MOBILE_DEVELOPER("모바일 개발자"),
	BLOCKCHAIN_DEVELOPER("블록체인 개발자"),
	GAME_DEVELOPER("게임 개발자"),
	SOFTWARE_ENGINEER("소프트웨어 엔지니어"),
	DEVOPS_ENGINEER("데브옵스 엔지니어"),
	NETWORK_ENGINEER("네트워크 엔지니어"),
	EMBEDDED_ENGINEER("임베디드 엔지니어"),
	SECURITY_ENGINEER("보안 엔지니어"),
	QA_ENGINEER("QA 엔지니어"),

	AI_ENGINEER("AI 엔지니어"),
	ML_ENGINEER("머신러닝 엔지니어"),
	MLOPS_ENGINEER("MLOps 엔지니어"),
	DATA_ENGINEER("데이터 엔지니어"),
	DATA_SCIENTIST("데이터 사이언티스트"),
	DATA_ANALYST("데이터 애널리스트"),
	BI_ANALYST("BI 애널리스트"),
	RESEARCHER("연구원"),

	DESIGNER("디자이너"),
	UX_DESIGNER("UX 디자이너"),
	UI_DESIGNER("UI 디자이너"),
	GRAPHIC_DESIGNER("그래픽 디자이너"),

	PRODUCT_MANAGER("프로덕트 매니저"),
	PROJECT_MANAGER("프로젝트 매니저"),
	PLANNER("서비스/기획자"),
	MARKETER("마케터"),
	STRATEGY("전략/기획"),
	OPERATION_MANAGER("운영 매니저"),

	STUDENT("학생"),
	ENTREPRENEUR("창업가"),
	ETC("기타");

	private final String description;
}
