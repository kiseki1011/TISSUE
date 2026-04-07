package com.tissue.feature.member.web.request;

import com.tissue.shared.enums.SupportedLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Update preferred language request")
public record UpdateMemberLanguageRequest(@NotNull SupportedLanguage language) {}
