package com.tissue.member.adapter.in.web.dto.request;

import com.tissue.common.enums.SupportedLanguage;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberLanguageRequest(@NotNull SupportedLanguage language) {}
