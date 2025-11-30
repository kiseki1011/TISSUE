package com.tissue.api.sprint.infrastructure.repository;

import org.springframework.data.repository.Repository;

import com.tissue.api.sprint.domain.model.Sprint;

public interface SprintCommandRepository extends Repository<Sprint, Long> {

	Sprint save(Sprint sprint);
}
