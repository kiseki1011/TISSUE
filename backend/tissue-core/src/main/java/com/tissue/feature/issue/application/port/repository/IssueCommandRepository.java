package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.Issue;
import org.springframework.data.repository.Repository;

public interface IssueCommandRepository extends Repository<Issue, Long> {

    Issue save(Issue issue);
}
