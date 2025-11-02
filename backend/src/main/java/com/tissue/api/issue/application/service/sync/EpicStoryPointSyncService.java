package com.tissue.api.issue.application.service.sync;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EpicStoryPointSyncService { // TODO: EpicStoryPointSyncService 대신 EpicStoryPointSyncHelper라는 이름을 사용할까?

	private final IssueQueryRepository issueQueryRepo;

	public void recalculateStoryPoint(@Nullable Issue issue) {
		if (issue == null) {
			return;
		}
		if (issue.isNotEpic()) {
			return;
		}

		int totalStoryPoints = issueQueryRepo.sumChildrenStoryPoints(
			issue.getWorkspaceKey(),
			issue.getKey()
		);

		issue.recalculateEpicStoryPoint(totalStoryPoints);
	}
}
