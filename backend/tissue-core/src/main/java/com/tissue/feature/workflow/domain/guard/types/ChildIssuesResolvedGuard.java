package com.tissue.feature.workflow.domain.guard.types;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.exception.UnresolvedChildIssuesException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChildIssuesResolvedGuard implements TransitionGuard {

    private final IssueQueryRepository issueQueryRepository;

    @Override
    public GuardType getType() {
        return GuardType.CHILD_ISSUES_RESOLVE_REQUIRED;
    }

    @Override
    public void evaluate(GuardContext context) {
        Issue issue = context.getIssue();

        List<String> unresolvedKeys =
                issueQueryRepository.findUnresolvedChildKeys(issue.getId(), StateCategory.terminalCategories());

        if (!unresolvedKeys.isEmpty()) {
            throw new UnresolvedChildIssuesException(issue.getKey(), unresolvedKeys);
        }
    }

    @Override
    public void validateParams(Map<String, Object> params, GuardType guardType) {}
}
