package com.tissue.feature.organization.position.application.dto.request;

import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreatePositionCommand(Name name, @Nullable String description, ColorType color) {}
