package com.tissue.feature.member.application.port.usecase;

import com.tissue.feature.member.application.dto.AgentResponse;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface AgentUseCase {

    AgentResponse createAgent(Long ownerId, String name, @Nullable String declaredModel);

    List<AgentResponse> listAgents(Long ownerId);

    void deactivateAgent(Long ownerId, Long agentId);
}
