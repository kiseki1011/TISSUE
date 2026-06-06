package com.tissue.feature.issue.application.service.finder;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.exception.IssueNotFoundException;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFinder {

    private final IssueQueryRepository issueQueryRepository;

    public Issue getWithProjectByIssueKey(String issueKey) {
        return issueQueryRepository
                .findWithProjectByKey(issueKey)
                .orElseThrow(() -> new IssueNotFoundException(issueKey));
    }

    public Issue getWithProjectAndIssueTypeByIssueKey(String issueKey) {
        return issueQueryRepository
                .findWithProjectAndIssueTypeByKey(issueKey)
                .orElseThrow(() -> new IssueNotFoundException(issueKey));
    }

    public Issue getWithProjectIssueTypeAndFieldsByIssueKey(String issueKey) {
        return issueQueryRepository
                .findWithProjectAndIssueTypeAndFieldsByKey(issueKey)
                .orElseThrow(() -> new IssueNotFoundException(issueKey));
    }

    public Issue getDeletedWithProjectByIssueKey(String issueKey) {
        return issueQueryRepository
                .findDeletedWithProjectByKey(issueKey)
                .orElseThrow(() -> new IssueNotFoundException(issueKey));
    }

    public List<Issue> getAllByIssueKeys(Collection<String> issueKeys) {
        return issueQueryRepository.findByKeyIn(issueKeys);
    }

    public List<Issue> getAllBySprint(Sprint sprint) {
        return issueQueryRepository.findAllBySprint(sprint);
    }

    public List<Issue> getIncompleteIssuesBySprint(Sprint sprint) {
        return issueQueryRepository.findIncompleteIssuesBySprint(sprint, StateCategory.terminalCategories());
    }

    public List<String> getIncompleteIssueKeysBySprint(Sprint sprint) {
        return issueQueryRepository.findIncompleteIssueKeysBySprint(sprint, StateCategory.terminalCategories());
    }
}
