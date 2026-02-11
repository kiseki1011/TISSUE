package com.tissue.feature.issuetype.application.service.finder;

import com.tissue.feature.issuetype.application.port.repository.EnumFieldOptionQueryRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldQueryRepository;
import com.tissue.feature.issuetype.domain.EnumFieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.exception.EnumFieldOptionNotFoundException;
import com.tissue.feature.issuetype.domain.exception.IssueFieldNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldFinder {

    private final IssueFieldQueryRepository issueFieldQueryRepository;
    private final EnumFieldOptionQueryRepository optionQueryRepository;

    public IssueField getWithProjectAndIssueTypeBy(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId) {
        return issueFieldQueryRepository
                .findWithProjectAndIssueTypeByKeys(workspaceKey, projectKey, issueTypeId, issueFieldId)
                .orElseThrow(() -> new IssueFieldNotFoundException(projectKey, issueTypeId, issueFieldId));
    }

    public EnumFieldOption getWithHierarchyBy(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId, Long optionId) {
        return optionQueryRepository
                .findWithHierarchyByKeys(workspaceKey, projectKey, issueTypeId, issueFieldId, optionId)
                .orElseThrow(
                        () -> new EnumFieldOptionNotFoundException(projectKey, issueTypeId, issueFieldId, optionId));
    }

    public List<EnumFieldOption> getAllOptions(IssueField field) {
        return optionQueryRepository.findByIssueFieldOrderByPositionAsc(field);
    }

    public int countOptions(IssueField issueField) {
        return optionQueryRepository.countByIssueField(issueField);
    }
}
