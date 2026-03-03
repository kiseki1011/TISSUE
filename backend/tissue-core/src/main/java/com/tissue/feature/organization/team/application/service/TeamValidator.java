package com.tissue.feature.organization.team.application.service;

import static com.tissue.feature.organization.team.domain.exception.TeamErrorCode.DUPLICATE_TEAM_NAME;
import static com.tissue.feature.organization.team.domain.exception.TeamErrorCode.TEAM_IN_USE;

import com.tissue.feature.organization.team.application.port.repository.TeamQueryRepository;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
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
            throw new ResourceConflictException(DUPLICATE_TEAM_NAME);
        }
    }

    public void ensureDeletable(Team team) {
        if (teamQueryRepository.existsByWorkspaceMembers(team)) {
            throw new BadRequestException(TEAM_IN_USE);
        }
    }
}
