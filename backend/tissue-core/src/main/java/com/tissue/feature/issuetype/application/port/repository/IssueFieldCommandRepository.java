package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.IssueField;
import org.springframework.data.repository.Repository;

public interface IssueFieldCommandRepository extends Repository<IssueField, Long> {

    IssueField save(IssueField issueField);

    void delete(IssueField issueField);
}
