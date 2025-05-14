package com.tissue.api.issue.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.domain.model.IssueReviewer;

public interface IssueReviewerRepository extends JpaRepository<IssueReviewer, Long> {

	@EntityGraph(attributePaths = "reviewer")
	@Query("SELECT ir FROM Issue i JOIN i.reviewers ir WHERE i.issueKey = :issueKey AND ir.reviewer.id = :reviewerId")
	Optional<IssueReviewer> findByIssueKeyAndReviewerId(
		@Param("issueKey") String issueKey,
		@Param("reviewerId") Long reviewerId
	);
}
