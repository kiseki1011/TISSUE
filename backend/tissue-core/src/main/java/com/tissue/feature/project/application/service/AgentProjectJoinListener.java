package com.tissue.feature.project.application.service;

import com.tissue.feature.member.domain.event.AgentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgentProjectJoinListener {

    private final AgentProjectJoinService agentProjectJoinService;

    @EventListener
    public void onAgentCreated(AgentCreatedEvent event) {
        agentProjectJoinService.includeAgentIntoOwnerProjects(event.agentMemberId());
    }
}
