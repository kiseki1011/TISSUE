package com.tissue.team.application.service.finder;

import com.tissue.team.application.port.out.TeamQueryRepository;
import com.tissue.team.domain.Team;
import com.tissue.team.domain.exception.TeamExceptions;
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
                .orElseThrow(() -> TeamExceptions.notFound(teamId, workspace.getKey()));
    }

    public Team getBy(Long teamId, String workspaceKey) {
        return teamRepository
                .findByIdAndWorkspace_Key(teamId, workspaceKey)
                .orElseThrow(() -> TeamExceptions.notFound(teamId, workspaceKey));
    }
}
