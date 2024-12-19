package com.uranus.taskmanager.api.issue.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uranus.taskmanager.api.issue.domain.Issue;

public interface IssueRepository extends JpaRepository<Issue, Long> {

	@Query("SELECT i FROM Issue i JOIN FETCH i.workspace WHERE i.id = :id")
	Optional<Issue> findByIdWithWorkspace(@Param("id") Long id);

	Optional<Issue> findByIdAndWorkspaceCode(Long id, String workspaceCode);
}
