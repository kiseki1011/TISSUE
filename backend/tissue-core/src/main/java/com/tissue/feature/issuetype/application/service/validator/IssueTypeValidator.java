package com.tissue.feature.issuetype.application.service.validator;

import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.DUPLICATE_ISSUE_TYPE_NAME;
import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.ISSUE_TYPE_IN_USE;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

    private final IssueTypeRepository issueTypeQueryRepo;
    private final IssueQueryRepository issueQueryRepo;

    public void ensureUniqueLabel(Project project, Name name) {
        boolean duplicated =
                issueTypeQueryRepo.existsByName_NormalizedNameAndProject(name.getNormalizedName(), project);
        if (duplicated) {
            throw new ResourceConflictException(DUPLICATE_ISSUE_TYPE_NAME);
        }
    }

    public void ensureDeletable(IssueType type) {
        ensureTypeNotInUse(type);
    }

    private void ensureTypeNotInUse(IssueType issueType) {
        if (issueQueryRepo.existsByIssueType(issueType)) {
            throw new ResourceConflictException(ISSUE_TYPE_IN_USE);
        }
    }
}
