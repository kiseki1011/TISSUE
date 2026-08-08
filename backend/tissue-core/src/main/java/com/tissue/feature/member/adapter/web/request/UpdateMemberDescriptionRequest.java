package com.tissue.feature.member.adapter.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record UpdateMemberDescriptionRequest(
        @Schema(
                description = "Free-form description / bio. Send `null` to clear it.",
                maxLength = DESCRIPTION_MAX_LENGTH)
        @Nullable
        @Size(max = DESCRIPTION_MAX_LENGTH)
        String description) {}
