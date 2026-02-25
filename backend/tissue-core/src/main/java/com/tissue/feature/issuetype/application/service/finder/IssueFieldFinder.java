package com.tissue.feature.issuetype.application.service.finder;

import com.tissue.feature.issuetype.application.port.repository.FieldOptionRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.exception.EnumFieldOptionNotFoundException;
import com.tissue.feature.issuetype.domain.exception.IssueFieldNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldFinder {

    private final IssueFieldRepository issueFieldRepository;
    private final FieldOptionRepository optionQueryRepository;

    public IssueField getWithProjectAndIssueTypeBy(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId) {
        return issueFieldRepository
                .findWithProjectAndIssueTypeByKeys(workspaceKey, projectKey, issueTypeId, issueFieldId)
                .orElseThrow(() -> new IssueFieldNotFoundException(projectKey, issueTypeId, issueFieldId));
    }

    public FieldOption getWithHierarchyBy(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId, Long optionId) {
        return optionQueryRepository
                .findWithHierarchyByKeys(workspaceKey, projectKey, issueTypeId, issueFieldId, optionId)
                .orElseThrow(
                        () -> new EnumFieldOptionNotFoundException(projectKey, issueTypeId, issueFieldId, optionId));
    }
}
