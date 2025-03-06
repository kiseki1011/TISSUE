package com.tissue.api.issue.service.eventlistener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.event.IssueParentChangedEvent;
import com.tissue.api.issue.domain.event.IssueStatusChangedEvent;
import com.tissue.api.issue.domain.event.IssueStoryPointChangedEvent;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.domain.types.Epic;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EpicStoryPointUpdater {

	private final IssueRepository issueRepository;

	@EventListener
	@Transactional
	public void handleIssueStoryPointChanged(IssueStoryPointChangedEvent event) {
		updateParentEpicStoryPoint(event.getIssue());
	}

	@EventListener
	@Transactional
	public void handleIssueParentChanged(IssueParentChangedEvent event) {
		updateParentEpicStoryPoint(event.getIssue());
	}

	@EventListener
	@Transactional
	public void handleIssueStatusChanged(IssueStatusChangedEvent event) {
		updateParentEpicStoryPoint(event.getIssue());
	}

	private void updateParentEpicStoryPoint(Issue issue) {
		Issue parent = issue.getParentIssue();
		if (parent instanceof Epic parentEpic) {
			parentEpic.updateStoryPoint();
		}
	}
}
