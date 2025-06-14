package com.tissue.api.member.domain.model.enums;

import java.util.Locale;

import org.springframework.context.MessageSource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

// TODO: description에 message 사용
@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public enum JobType {
	DEVELOPER("developer"),
	BACKEND_DEVELOPER("backend_developer"),
	FRONTEND_DEVELOPER("frontend_developer"),
	FULLSTACK_DEVELOPER("fullstack_developer"),
	MOBILE_DEVELOPER("mobile_developer"),
	BLOCKCHAIN_DEVELOPER("blockchain_developer"),
	GAME_DEVELOPER("game_developer"),
	SOFTWARE_ENGINEER("software_engineer"),
	DEVOPS_ENGINEER("devops_engineer"),
	NETWORK_ENGINEER("network_engineer"),
	EMBEDDED_ENGINEER("embedded_engineer"),
	SECURITY_ENGINEER("security_engineer"),
	QA_ENGINEER("qa_engineer"),

	AI_ENGINEER("ai_engineer"),
	ML_ENGINEER("ml_engineer"),
	MLOPS_ENGINEER("mlops_engineer"),
	DATA_ENGINEER("data_engineer"),
	DATA_SCIENTIST("data_scientist"),
	DATA_ANALYST("data_analyst"),
	BI_ANALYST("bi_analyst"),
	RESEARCHER("researcher"),

	DESIGNER("designer"),
	UX_DESIGNER("ux_designer"),
	UI_DESIGNER("ui_designer"),
	GRAPHIC_DESIGNER("graphic_designer"),

	PRODUCT_MANAGER("product_manager"),
	PROJECT_MANAGER("project_manager"),

	ETC("etc");

	private final String messageKey;

	public String getDescription(MessageSource messageSource, Locale locale) {
		return messageSource.getMessage("jobtype." + messageKey, null, locale);
	}
}
