package com.tissue.issue.application.dto.response.info;

import com.tissue.issuetype.domain.EnumFieldOption;

public record EnumOptionInfo(
	Long id,
	String displayLabel
) {
	public static EnumOptionInfo of(EnumFieldOption option) {
		if (option == null) {
			return null;
		}
		return new EnumOptionInfo(option.getId(), option.getDisplayLabel());
	}
}
