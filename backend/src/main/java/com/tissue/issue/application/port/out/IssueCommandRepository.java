package com.tissue.issue.application.port.out;

import com.tissue.issue.domain.Issue;
import org.springframework.data.repository.Repository;

public interface IssueCommandRepository extends Repository<Issue, Long> {

    Issue save(Issue issue);
}
