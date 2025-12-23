package com.tissue.member.adapter.in.web.dto.request;

import com.tissue.common.validator.annotation.pattern.NamePattern;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberNameRequest(
	@NotBlank
	@NamePattern
	@Size(min = 2, max = 50)
	String newName
) {
}
