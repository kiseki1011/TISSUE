package com.tissue.api.issuetype.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.api.issuetype.domain.IssueField;

public interface IssueFieldCommandRepository extends Repository<IssueField, Long> {

	IssueField save(IssueField issueField);

	void delete(IssueField issueField);
}
