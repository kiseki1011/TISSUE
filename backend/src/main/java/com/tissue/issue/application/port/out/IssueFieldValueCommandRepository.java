package com.tissue.issue.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.issue.domain.IssueFieldValue;

public interface IssueFieldValueCommandRepository extends Repository<IssueFieldValue, Long> {
}
