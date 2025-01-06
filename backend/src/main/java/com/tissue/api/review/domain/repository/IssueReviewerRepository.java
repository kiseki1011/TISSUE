package com.tissue.api.review.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.review.domain.IssueReviewer;

public interface IssueReviewerRepository extends JpaRepository<IssueReviewer, Long> {
}
