package com.tissue.api.issue.service.eventlistener;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.types.Epic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EpicStoryPointUpdater {

	// @Async("epicTaskExecutor")
	// @Transactional(propagation = Propagation.REQUIRES_NEW)
	// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	// @EventListener
	// public void handleIssueStoryPointChanged(IssueStoryPointChangedEvent event) {
	// 	log.debug("Handling story point change for issue: {}", event.getIssue().getIssueKey());
	// 	updateParentEpicStoryPoint(event.getIssue());
	// }

	// @Async("epicTaskExecutor")
	// @Transactional(propagation = Propagation.REQUIRES_NEW)
	// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	// @EventListener
	// public void handleIssueParentChanged(IssueParentChangedEvent event) {
	// 	log.debug("Handling parent change for issue: {}", event.getIssue().getIssueKey());
	//
	// 	// 이전 Epic 부모 업데이트
	// 	if (event.getOldParent() instanceof Epic oldParentEpic) {
	// 		oldParentEpic.updateStoryPoint();
	// 		log.debug("Updated story points for old parent epic: {}", oldParentEpic.getIssueKey());
	// 	}
	//
	// 	// 새 Epic 부모 업데이트
	// 	updateParentEpicStoryPoint(event.getIssue());
	// }

	// @Async("epicTaskExecutor")
	// @Transactional(propagation = Propagation.REQUIRES_NEW)
	// @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	// @EventListener
	// public void handleIssueStatusChanged(IssueStatusChangedEvent event) {
	// 	log.debug("Handling status change for issue: {} from {} to {}",
	// 		event.getIssue().getIssueKey(), event.getOldStatus(), event.getNewStatus());
	//
	// 	if (event.isClosedStatusChange()) {
	// 		updateParentEpicStoryPoint(event.getIssue());
	// 	}
	// }

	private void updateParentEpicStoryPoint(Issue issue) {
		Issue parent = issue.getParentIssue();
		if (parent instanceof Epic parentEpic) {
			Integer beforeUpdate = parentEpic.getStoryPoint();
			parentEpic.updateStoryPoint();
			log.debug("Updated epic story points: {} -> {} for epic: {}",
				beforeUpdate, parentEpic.getStoryPoint(), parentEpic.getIssueKey());
		}
	}
}
