package com.tissue.api.issue.domain.port.out;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.tissue.api.issue.domain.IssueFieldValue;

public interface IssueFieldValueCommandRepository extends Repository<IssueFieldValue, Long> {

	List<IssueFieldValue> saveAll(Iterable<IssueFieldValue> updateValues);
}
