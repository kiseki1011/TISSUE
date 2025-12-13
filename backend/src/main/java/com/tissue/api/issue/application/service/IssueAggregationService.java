package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.IssueCountStats;
import com.tissue.api.issue.application.dto.IssuePointStats;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueAggregationService {

	private final IssueQueryRepository issueQueryRepository;

	@Transactional
	public void syncStatistics(Long issueId) {
		issueQueryRepository.findById(issueId).ifPresent(issue -> {
			syncEpicStoryPoint(issue);
			syncProgress(issue);
		});
	}

	// TODO: softDelete 된 자식들은 계산에서 제외해야하지 않을까?
	private void syncEpicStoryPoint(Issue issue) {
		if (issue.getHierarchy().isEpic()) {
			Integer totalStoryPoint = issueQueryRepository.sumChildrenStoryPoints(issue.getId());
			issue.recalculateEpicStoryPoint(totalStoryPoint != null ? totalStoryPoint : 0);
		}
	}

	// TODO: softDelete 된 자식들은 계산에서 제외해야하지 않을까?
	private void syncProgress(Issue issue) {
		IssueCountStats countStats = issueQueryRepository.getChildIssueStats(issue.getId());
		int countBasedProgress = calculatePercent(countStats.doneCount(), countStats.totalCount());

		Integer pointBasedProgress = null;
		if (issue.getHierarchy().isEpic()) {
			IssuePointStats pointStats = issueQueryRepository.getChildPointStats(issue.getId());
			pointBasedProgress = calculatePercent(pointStats.donePoints(), pointStats.totalPoints());
		}

		issue.updateProgress(countBasedProgress, pointBasedProgress);
	}

	private int calculatePercent(long done, long total) {
		if (total == 0) {
			return 0;
		}
		return (int)((double)done / total * 100);
	}

	// @Transactional
	// public void syncEpicStoryPoint(Long issueId) {
	// 	issueQueryRepository.findById(issueId)
	// 		.filter(issue -> issue.getHierarchy().isEpic())
	// 		.ifPresent(issue -> {
	// 			Integer total = issueQueryRepository.sumChildrenStoryPoints(issue.getId());
	// 			issue.recalculateEpicStoryPoint(total != null ? total : 0);
	// 		});
	// }
	//
	// @Transactional
	// public void syncProgress(Long issueId) {
	// 	issueQueryRepository.findById(issueId)
	// 		.ifPresent(issue -> {
	// 			IssueCountStats countStats = issueQueryRepository.getChildIssueStats(issue.getId());
	// 			Integer countBased = calculatePercent(countStats.doneCount(), countStats.totalCount());
	//
	// 			Integer pointBased = null;
	//
	// 			if (issue.getHierarchy().isEpic()) {
	// 				IssuePointStats pointStats = issueQueryRepository.getChildPointStats(issue.getId());
	// 				pointBased = calculatePercent(pointStats.donePoints(), pointStats.totalPoints());
	// 			}
	//
	// 			issue.updateProgress(countBased, pointBased);
	// 		});

	// }
}
