package com.tissue.feature.organization.team.application.dto.request;

import com.tissue.shared.enums.ColorType;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTeamCommand(
        JsonNullable<String> name, JsonNullable<String> description, JsonNullable<ColorType> color) {}
