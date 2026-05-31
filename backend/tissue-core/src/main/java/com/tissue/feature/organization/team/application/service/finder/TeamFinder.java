package com.tissue.feature.organization.team.application.service.finder;

import com.tissue.feature.organization.team.application.port.repository.TeamRepository;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.feature.organization.team.domain.exception.TeamNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamFinder {

    private final TeamRepository teamRepository;

    public Team getById(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new TeamNotFoundException(teamId));
    }
}
