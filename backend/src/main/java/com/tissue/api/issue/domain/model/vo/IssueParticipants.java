package com.tissue.api.issue.domain.model.vo;

import java.util.HashSet;
import java.util.Set;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueReviewer;
import com.tissue.api.issue.domain.model.IssueSubscriber;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueParticipants {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private WorkspaceMember reporter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private WorkspaceMember assignee;

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueReviewer> reviewers = new HashSet<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueSubscriber> subscribers = new HashSet<>();

	public static IssueParticipants init(@NonNull WorkspaceMember reporter) {
		IssueParticipants participants = new IssueParticipants();
		participants.reporter = reporter;

		return participants;
	}

	public void changeReporter(@NonNull WorkspaceMember reporter) {
		this.reporter = reporter;
	}

	public void assignTo(@NonNull WorkspaceMember assignee) {
		this.assignee = assignee;
	}

	public void unassign() {
		this.assignee = null;
	}

	public void addReviewer(@NonNull WorkspaceMember workspaceMember, @NonNull Issue issue) {
		if (isReviewer(workspaceMember)) {
			return;
		}
		reviewers.add(new IssueReviewer(workspaceMember, issue));
	}

	public void removeReviewer(@NonNull WorkspaceMember workspaceMember) {
		reviewers.removeIf(r -> r.getReviewer().equals(workspaceMember));
	}

	public void addSubscriber(@NonNull WorkspaceMember workspaceMember, @NonNull Issue issue) {
		if (isSubscriber(workspaceMember)) {
			return;
		}
		subscribers.add(new IssueSubscriber(workspaceMember, issue));
	}

	public void removeSubscriber(@NonNull WorkspaceMember workspaceMember) {
		subscribers.removeIf(s -> s.getSubscriber().equals(workspaceMember));
	}

	public void clear() {
		unassign();
		this.reviewers.clear();
		this.subscribers.clear();
	}

	public boolean isReporter(WorkspaceMember member) {
		return reporter.equals(member);
	}

	public boolean isAssignee(WorkspaceMember member) {
		return assignee != null && assignee.equals(member);
	}

	public boolean isReviewer(WorkspaceMember member) {
		return reviewers.stream()
			.anyMatch(r -> r.getReviewer().equals(member));
	}

	public boolean isSubscriber(WorkspaceMember member) {
		return subscribers.stream()
			.anyMatch(s -> s.getSubscriber().equals(member));
	}

}
