package com.tissue.api.sprint.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.sprint.domain.Sprint;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

	Optional<Sprint> findBySprintKeyAndWorkspaceCode(String sprintKey, String workspaceCode);
}
