package com.tissue.team.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.team.domain.Team;

public interface TeamCommandRepository extends Repository<Team, Long> {

	Team save(Team team);

	void delete(Team team);
}
