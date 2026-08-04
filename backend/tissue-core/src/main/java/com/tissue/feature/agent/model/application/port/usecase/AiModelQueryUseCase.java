package com.tissue.feature.agent.model.application.port.usecase;

import com.tissue.feature.agent.model.application.dto.response.AiModelSummary;
import java.util.List;

public interface AiModelQueryUseCase {

    List<AiModelSummary> getModels(Long actorMemberId);
}
