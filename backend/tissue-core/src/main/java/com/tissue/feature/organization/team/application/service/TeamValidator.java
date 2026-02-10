package com.tissue.feature.organization.team.application.service;

import com.tissue.feature.organization.team.application.port.out.TeamQueryRepository;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.feature.organization.team.domain.exception.DuplicateTeamNameException;
import com.tissue.feature.organization.team.domain.exception.TeamInUseException;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.vo.Name;
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
