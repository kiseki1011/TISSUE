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

    public IssueField getWithProjectAndIssueType(String workspaceKey, Long issueFieldId) {
        return issueFieldRepository
                .findWithProjectAndIssueTypeByWorkspaceKeyAndId(workspaceKey, issueFieldId)
                .orElseThrow(() -> new IssueFieldNotFoundException(issueFieldId));
    }

    public FieldOption getWithHierarchy(String workspaceKey, Long issueFieldId, Long optionId) {
        return optionQueryRepository
                .findWithHierarchyByWorkspaceKeyAndFieldIdAndId(workspaceKey, issueFieldId, optionId)
                .orElseThrow(() -> new EnumFieldOptionNotFoundException(issueFieldId, optionId));
    }
}
