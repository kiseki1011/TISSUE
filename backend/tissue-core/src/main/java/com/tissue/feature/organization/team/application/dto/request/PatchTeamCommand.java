package com.tissue.feature.organization.team.application.dto.request;

import com.tissue.shared.enums.ColorType;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchTeamCommand(
        JsonNullable<String> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
