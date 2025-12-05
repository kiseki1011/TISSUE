package com.tissue.api.issuetype.application.port.in;

import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface IssueFieldQueryUseCase {
}
