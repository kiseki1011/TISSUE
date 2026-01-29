package com.tissue.team.application.service;

import com.tissue.team.application.port.out.TeamQueryRepository;
import com.tissue.team.domain.Team;
import com.tissue.team.domain.exception.TeamNotFoundException;
import com.tissue.workspace.domain.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamFinder {

    private final TeamQueryRepository teamRepository;

    public Team getBy(Long teamId, Workspace workspace) {
        return teamRepository
                .findByIdAndWorkspace(teamId, workspace)
                .orElseThrow(() -> new TeamNotFoundException(teamId, workspace.getKey()));
    }

    public Team getBy(Long teamId, String workspaceKey) {
        return teamRepository
                .findByIdAndWorkspace_Key(teamId, workspaceKey)
                .orElseThrow(() -> new TeamNotFoundException(teamId, workspaceKey));
    }
}
