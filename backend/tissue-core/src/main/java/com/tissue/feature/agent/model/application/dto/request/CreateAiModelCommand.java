package com.tissue.feature.agent.model.application.dto.request;

import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateAiModelCommand(Name name, @Nullable String description, ColorType color) {}
