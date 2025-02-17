package com.tissue.api.sprint.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.sprint.domain.Sprint;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
}
