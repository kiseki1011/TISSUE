package com.tissue.feature.agent.application.port.usecase;

import com.tissue.feature.agent.application.dto.AgentResponse;
import com.tissue.feature.agent.application.dto.CreateAgentCommand;
import com.tissue.feature.agent.application.dto.PatchAgentCommand;
import java.util.List;

public interface AgentUseCase {

    AgentResponse createAgent(Long ownerId, CreateAgentCommand cmd);

    List<AgentResponse> listAgents(Long ownerId);

    void updateAgent(Long ownerId, Long agentId, PatchAgentCommand cmd);

    void deactivateAgent(Long ownerId, Long agentId);
}
