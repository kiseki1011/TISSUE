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

    public IssueField getWithIssueType(Long issueFieldId) {
        return issueFieldRepository
                .findWithIssueTypeById(issueFieldId)
                .orElseThrow(() -> new IssueFieldNotFoundException(issueFieldId));
    }

    public FieldOption getWithHierarchy(Long issueFieldId, Long optionId) {
        return optionQueryRepository
                .findWithHierarchyByFieldIdAndId(issueFieldId, optionId)
                .orElseThrow(() -> new EnumFieldOptionNotFoundException(issueFieldId, optionId));
    }
}
