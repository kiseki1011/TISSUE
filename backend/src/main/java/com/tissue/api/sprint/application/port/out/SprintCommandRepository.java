package com.tissue.api.sprint.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.api.sprint.domain.Sprint;

public interface SprintCommandRepository extends Repository<Sprint, Long> {

	Sprint save(Sprint sprint);
}
