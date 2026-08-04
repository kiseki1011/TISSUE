package com.tissue.security.application.service;

import com.tissue.feature.agent.application.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnedAgentDeactivationService {

    private final AgentService agentService;
    private final PersonalAccessTokenService personalAccessTokenService;

    @Transactional
    public void deactivateAgentsOf(Long ownerId) {
        agentService
                .deactivateAllOwnedBy(ownerId)
                .forEach(agent -> personalAccessTokenService.revokeAllFor(agent.getId()));
    }
}
