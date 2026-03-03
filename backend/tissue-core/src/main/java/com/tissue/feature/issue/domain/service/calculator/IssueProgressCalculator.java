package com.tissue.feature.issue.domain.service.calculator;

import com.tissue.feature.issue.domain.Issue;
import org.springframework.stereotype.Component;

/**
 * Domain service responsible for calculating issue progress and story points.
 */
@Component
public class IssueProgressCalculator {

    public void calculateAndUpdateProgress(
            Issue issue, long doneCount, long totalCount, long donePoints, long totalPoints) {
        int countBased = calculatePercent(doneCount, totalCount);

        Integer pointBased = null;
        if (issue.getHierarchy().isEpic()) {
            pointBased = calculatePercent(donePoints, totalPoints);
        }

        issue.updateProgress(countBased, pointBased);
    }

    public void calculateAndUpdateEpicStoryPoint(Issue issue, int totalChildrenStoryPoints) {
        if (issue.getHierarchy().isNotEpic()) {
            return;
        }
        issue.recalculateEpicStoryPoint(totalChildrenStoryPoints);
    }

    private int calculatePercent(long done, long total) {
        if (total == 0) {
            return 0;
        }
        return (int) ((double) done / total * 100);
    }
}
