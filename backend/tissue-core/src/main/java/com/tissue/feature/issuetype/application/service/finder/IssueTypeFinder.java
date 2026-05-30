package com.tissue.feature.issuetype.application.service.finder;

import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

    private final IssueTypeRepository issueTypeRepository;

    public IssueType getById(Long issueTypeId) {
        return issueTypeRepository.findById(issueTypeId).orElseThrow(() -> new IssueTypeNotFoundException(issueTypeId));
    }

    public IssueType getWithWorkflowBy(Long issueTypeId) {
        return issueTypeRepository
                .findWithWorkflowById(issueTypeId)
                .orElseThrow(() -> new IssueTypeNotFoundException(issueTypeId));
    }
}
