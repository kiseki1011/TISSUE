package com.tissue.member.web.request;

import com.tissue.enums.SupportedLanguage;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberLanguageRequest(@NotNull SupportedLanguage language) {}
