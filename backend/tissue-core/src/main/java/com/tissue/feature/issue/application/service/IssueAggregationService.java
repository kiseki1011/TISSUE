package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.IssueCountStats;
import com.tissue.feature.issue.application.dto.IssuePointStats;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.service.calculator.IssueProgressCalculator;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueAggregationService {

    private final IssueQueryRepository issueQueryRepository;
    private final IssueProgressCalculator progressCalculator;

    @Transactional
    public void syncStatistics(String workspaceKey, String issueKey) {
        issueQueryRepository.findByKeyAndWorkspaceKey(issueKey, workspaceKey).ifPresent(issue -> {
            syncEpicStoryPoint(issue);
            syncProgress(issue);
        });
    }

    private void syncEpicStoryPoint(Issue issue) {
        if (issue.getHierarchy().isEpic()) {
            Integer totalStoryPoint = issueQueryRepository.sumChildrenStoryPoints(issue.getId());
            progressCalculator.calculateAndUpdateEpicStoryPoint(issue, totalStoryPoint);
        }
    }

    private void syncProgress(Issue issue) {
        IssueCountStats countStats =
                issueQueryRepository.getChildIssueStats(issue.getId(), StateCategory.COMPLETED, StateCategory.ABORTED);

        long donePoints = 0;
        long totalPoints = 0;

        if (issue.getHierarchy().isEpic()) {
            IssuePointStats pointStats = issueQueryRepository.getChildPointStats(
                    issue.getId(), StateCategory.COMPLETED, StateCategory.ABORTED);
            donePoints = pointStats.getDonePoints();
            totalPoints = pointStats.getTotalPoints();
        }

        progressCalculator.calculateAndUpdateProgress(
                issue, countStats.getDoneCount(), countStats.getTotalCount(), donePoints, totalPoints);
    }
}
