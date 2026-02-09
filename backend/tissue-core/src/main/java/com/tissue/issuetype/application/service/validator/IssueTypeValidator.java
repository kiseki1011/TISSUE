package com.tissue.issuetype.application.service.validator;

import com.tissue.global.vo.Name;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.DuplicateIssueTypeNameException;
import com.tissue.issuetype.domain.exception.IssueTypeInUseException;
import com.tissue.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

    private final IssueTypeQueryRepository issueTypeQueryRepo;
    private final IssueQueryRepository issueQueryRepo;

    public void ensureUniqueLabel(Project project, Name name) {
        boolean duplicated = issueTypeQueryRepo.existsByName_NormalizedAndProject(name.getNormalized(), project);
        if (duplicated) {
            throw new DuplicateIssueTypeNameException(name, project);
        }
    }

    public void ensureDeletable(IssueType type) {
        ensureTypeNotInUse(type);
    }

    private void ensureTypeNotInUse(IssueType issueType) {
        if (issueQueryRepo.existsByIssueType(issueType)) {
            throw new IssueTypeInUseException(issueType);
        }
    }
}
