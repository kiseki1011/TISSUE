package com.tissue.feature.agent.model.application.dto.response;

import com.tissue.feature.agent.model.domain.AiModel;
import com.tissue.shared.enums.ColorType;

public record AiModelSummary(Long id, String name, String description, ColorType color) {

    public static AiModelSummary from(AiModel model) {
        return new AiModelSummary(model.getId(), model.getName(), model.getDescription(), model.getColor());
    }
}
