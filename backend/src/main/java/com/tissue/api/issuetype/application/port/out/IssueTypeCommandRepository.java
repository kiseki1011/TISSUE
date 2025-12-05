package com.tissue.api.issuetype.application.port.out;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tissue.api.issuetype.domain.IssueType;

public interface IssueTypeCommandRepository extends Repository<IssueType, Long> {

	IssueType save(IssueType issueType);

	List<IssueType> saveAll(Iterable<IssueType> issueTypes);

	void delete(IssueType issueType);
}
