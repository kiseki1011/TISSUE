package com.tissue.issuetype.application.port.out;

import com.tissue.issuetype.domain.IssueField;
import org.springframework.data.repository.Repository;

public interface IssueFieldCommandRepository extends Repository<IssueField, Long> {

    IssueField save(IssueField issueField);

    void delete(IssueField issueField);
}
