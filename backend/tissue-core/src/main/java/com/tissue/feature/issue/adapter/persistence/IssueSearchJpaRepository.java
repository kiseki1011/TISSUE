package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.domain.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IssueSearchJpaRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {}
