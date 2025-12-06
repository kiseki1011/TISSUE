package com.tissue.api.issue.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.api.issue.domain.IssueFieldValue;

public interface IssueFieldValueCommandRepository extends Repository<IssueFieldValue, Long> {
}
