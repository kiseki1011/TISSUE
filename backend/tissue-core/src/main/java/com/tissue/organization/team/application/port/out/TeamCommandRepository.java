package com.tissue.organization.team.application.port.out;

import com.tissue.organization.team.domain.Team;
import org.springframework.data.repository.Repository;

public interface TeamCommandRepository extends Repository<Team, Long> {

    Team save(Team team);

    void delete(Team team);
}
