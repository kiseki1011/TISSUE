package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.IssueCountStats;
import com.tissue.issue.application.dto.IssuePointStats;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.service.calculator.IssueProgressCalculator;
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
        IssueCountStats countStats = issueQueryRepository.getChildIssueStats(issue.getId());

        long donePoints = 0;
        long totalPoints = 0;

        if (issue.getHierarchy().isEpic()) {
            IssuePointStats pointStats = issueQueryRepository.getChildPointStats(issue.getId());
            donePoints = pointStats.donePoints();
            totalPoints = pointStats.totalPoints();
        }

        progressCalculator.calculateAndUpdateProgress(
                issue, countStats.doneCount(), countStats.totalCount(), donePoints, totalPoints);
    }
}
