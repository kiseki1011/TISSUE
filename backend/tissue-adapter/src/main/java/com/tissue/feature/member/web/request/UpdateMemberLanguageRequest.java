package com.tissue.feature.member.web.request;

import com.tissue.shared.enums.SupportedLanguage;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberLanguageRequest(@NotNull SupportedLanguage language) {}
