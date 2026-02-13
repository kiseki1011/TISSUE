package com.tissue.feature.issuetype.application.service.finder;

import com.tissue.feature.issuetype.application.port.repository.EnumFieldOptionRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
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

    private final IssueFieldRepository issueFieldRepository;
    private final EnumFieldOptionRepository optionQueryRepository;

    public IssueField getWithProjectAndIssueTypeBy(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId) {
        return issueFieldRepository
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
