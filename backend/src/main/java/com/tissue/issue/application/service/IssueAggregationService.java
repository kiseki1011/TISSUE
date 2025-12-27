package com.tissue.issue.application.service;

import com.tissue.issue.application.dto.IssueCountStats;
import com.tissue.issue.application.dto.IssuePointStats;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueAggregationService {

    private final IssueQueryRepository issueQueryRepository;

    @Transactional
    public void syncStatistics(Long issueId) {
        issueQueryRepository
                .findById(issueId)
                .ifPresent(
                        issue -> {
                            syncEpicStoryPoint(issue);
                            syncProgress(issue);
                        });
    }

    private void syncEpicStoryPoint(Issue issue) {
        if (issue.getHierarchy().isEpic()) {
            Integer totalStoryPoint = issueQueryRepository.sumChildrenStoryPoints(issue.getId());
            issue.recalculateEpicStoryPoint(totalStoryPoint);
        }
    }

    private void syncProgress(Issue issue) {
        IssueCountStats countStats = issueQueryRepository.getChildIssueStats(issue.getId());
        int countBasedProgress = calculatePercent(countStats.doneCount(), countStats.totalCount());

        Integer pointBasedProgress = null;
        if (issue.getHierarchy().isEpic()) {
            IssuePointStats pointStats = issueQueryRepository.getChildPointStats(issue.getId());
            pointBasedProgress =
                    calculatePercent(pointStats.donePoints(), pointStats.totalPoints());
        }

        issue.updateProgress(countBasedProgress, pointBasedProgress);
    }

    private int calculatePercent(long done, long total) {
        if (total == 0) {
            return 0;
        }
        return (int) ((double) done / total * 100);
    }
}
