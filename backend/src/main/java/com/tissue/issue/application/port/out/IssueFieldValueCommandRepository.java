package com.tissue.issue.application.port.out;

import com.tissue.issue.domain.IssueFieldValue;
import org.springframework.data.repository.Repository;

public interface IssueFieldValueCommandRepository extends Repository<IssueFieldValue, Long> {}
