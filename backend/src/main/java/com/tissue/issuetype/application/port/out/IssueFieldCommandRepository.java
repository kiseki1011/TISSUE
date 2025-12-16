package com.tissue.issuetype.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.issuetype.domain.IssueField;

public interface IssueFieldCommandRepository extends Repository<IssueField, Long> {

	IssueField save(IssueField issueField);

	void delete(IssueField issueField);
}
