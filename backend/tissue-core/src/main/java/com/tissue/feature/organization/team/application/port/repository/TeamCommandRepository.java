package com.tissue.feature.organization.team.application.port.repository;

import com.tissue.feature.organization.team.domain.Team;
import org.springframework.data.repository.Repository;

public interface TeamCommandRepository extends Repository<Team, Long> {

    Team save(Team team);

    void delete(Team team);
}
