package com.tissue.team.application.service;

import com.tissue.global.vo.Name;
import com.tissue.team.application.port.out.TeamQueryRepository;
import com.tissue.team.domain.Team;
import com.tissue.team.domain.exception.DuplicateTeamNameException;
import com.tissue.team.domain.exception.TeamInUseException;
import com.tissue.workspace.domain.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamValidator {

    private final TeamQueryRepository teamQueryRepository;

    public void ensureUniqueName(Workspace workspace, String name) {
        String normalizedName = Name.of(name).getNormalized();

        if (teamQueryRepository.existsByWorkspaceAndName_Normalized(workspace, normalizedName)) {
            throw new DuplicateTeamNameException(name, workspace.getKey());
        }
    }

    public void ensureDeletable(Team team) {
        if (teamQueryRepository.existsByWorkspaceMembers(team)) {
            throw new TeamInUseException(team);
        }
    }
}
