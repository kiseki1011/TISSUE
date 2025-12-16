package com.tissue.issuetype.application.port.in;

import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface IssueTypeQueryUseCase {
}
