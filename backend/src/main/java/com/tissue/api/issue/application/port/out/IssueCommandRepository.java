package com.tissue.api.issue.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.api.issue.domain.Issue;

public interface IssueCommandRepository extends Repository<Issue, Long> {

	Issue save(Issue issue);
}
