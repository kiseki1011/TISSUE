package com.tissue.issuetype.application.service.finder;

import com.tissue.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// TODO: should i just integrate this into IssueTypeFinder?
@Component
@RequiredArgsConstructor
public class IssueFieldFinder {

    private final IssueFieldQueryRepository issueFieldRepo;

    public IssueField findBy(Long issueFieldId, IssueType issueType) {
        return issueFieldRepo
                .findByIdAndIssueType(issueFieldId, issueType)
                .orElseThrow(() -> IssueTypeExceptions.fieldNotFound(issueFieldId, issueType));
    }

    public List<IssueField> findByIssueType(IssueType issueType) {
        return issueFieldRepo.findByIssueType(issueType);
    }
}
