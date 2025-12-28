package com.tissue.team.application.port.out;

import com.tissue.team.domain.Team;
import org.springframework.data.repository.Repository;

public interface TeamCommandRepository extends Repository<Team, Long> {

    Team save(Team team);

    void delete(Team team);
}
