package com.tissue.api.issue.service.eventpublisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.event.IssueParentChangedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.issue.domain.event.IssueStoryPointChangedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IssueEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	public void publishIssueCreated(
		Issue issue,
		Long triggeredBy
	) {
		IssueCreatedEvent event = new IssueCreatedEvent(issue, triggeredBy);
		eventPublisher.publishEvent(event);
	}

	public void publishStatusChanged(
		Issue issue,
		IssueStatus oldStatus,
		IssueStatus newStatus,
		Long triggeredBy
	) {
		IssueStatusChangedEvent event = new IssueStatusChangedEvent(
			issue,
			oldStatus,
			newStatus,
			triggeredBy
		);

		// CLOSED 상태 변경과 관련된 경우만 발행
		if (event.isClosedStatusChange() && shouldPublishForEpicStoryPoint(issue)) {
			eventPublisher.publishEvent(event);
		}
	}

	public void publishStoryPointChanged(
		Issue issue,
		Integer oldStoryPoint,
		Integer newStoryPoint,
		Long triggeredBy
	) {
		IssueStoryPointChangedEvent event = new IssueStoryPointChangedEvent(
			issue,
			oldStoryPoint,
			newStoryPoint,
			triggeredBy
		);

		// 스토리 포인트가 실제로 변경되었고, Epic 부모가 있는 경우에만 발행
		if (event.hasStoryPointChanged() && shouldPublishForEpicStoryPoint(issue)) {
			eventPublisher.publishEvent(event);
		}
	}

	public void publishParentChanged(
		Issue issue,
		Issue oldParent,
		Issue newParent,
		Long triggeredBy
	) {
		IssueParentChangedEvent event = new IssueParentChangedEvent(
			issue,
			oldParent,
			newParent,
			triggeredBy
		);

		// Epic 관련 변경이고 Sub-task가 아닌 경우에만 발행
		if (event.hasEpicParentChanged() && shouldPublishForEpicStoryPoint(issue)) {
			eventPublisher.publishEvent(event);
		}
	}

	private boolean shouldPublishForEpicStoryPoint(Issue issue) {
		return issue.getType() != IssueType.SUB_TASK;
	}
}
