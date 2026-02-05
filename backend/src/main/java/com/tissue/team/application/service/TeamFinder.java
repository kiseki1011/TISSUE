package com.tissue.team.application.service;

import com.tissue.team.application.port.out.TeamQueryRepository;
import com.tissue.team.domain.Team;
import com.tissue.team.domain.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamFinder {

    private final TeamQueryRepository teamRepository;

    public Team getBy(String workspaceKey, Long teamId) {
        return teamRepository
                .findByWorkspace_KeyAndId(workspaceKey, teamId)
                .orElseThrow(() -> new TeamNotFoundException(workspaceKey, teamId));
    }

    public Team getWithWorkspaceBy(String workspaceKey, Long teamId) {
        return teamRepository
                .findWithWorkspaceByKeys(workspaceKey, teamId)
                .orElseThrow(() -> new TeamNotFoundException(workspaceKey, teamId));
    }
}
