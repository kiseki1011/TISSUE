package com.tissue.api.issue.domain;

import java.util.HashSet;
import java.util.Set;

import com.tissue.api.workspace.domain.WorkspaceMember;

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

	public static IssueParticipants of(@NonNull WorkspaceMember reporter) {
		IssueParticipants participants = new IssueParticipants();
		participants.reporter = reporter;

		return participants;
	}

	void changeReporter(@NonNull WorkspaceMember reporter) {
		this.reporter = reporter;
	}

	void assignTo(@NonNull WorkspaceMember assignee) {
		this.assignee = assignee;
	}

	void unassign() {
		this.assignee = null;
	}

	void addReviewer(@NonNull WorkspaceMember workspaceMember, @NonNull Issue issue) {
		if (isReviewer(workspaceMember)) {
			return;
		}
		reviewers.add(new IssueReviewer(workspaceMember, issue));
	}

	void removeReviewer(@NonNull WorkspaceMember workspaceMember) {
		reviewers.removeIf(r -> r.getReviewer().equals(workspaceMember));
	}

	void addSubscriber(@NonNull WorkspaceMember workspaceMember, @NonNull Issue issue) {
		if (isSubscriber(workspaceMember)) {
			return;
		}
		subscribers.add(new IssueSubscriber(workspaceMember, issue));
	}

	void removeSubscriber(@NonNull WorkspaceMember workspaceMember) {
		subscribers.removeIf(s -> s.getSubscriber().equals(workspaceMember));
	}

	void clear() {
		unassign();
		this.reviewers.clear();
		this.subscribers.clear();
	}

	boolean isReporter(WorkspaceMember member) {
		return reporter.equals(member);
	}

	boolean isAssignee(WorkspaceMember member) {
		return assignee != null && assignee.equals(member);
	}

	boolean isReviewer(WorkspaceMember member) {
		return reviewers.stream()
			.anyMatch(r -> r.getReviewer().equals(member));
	}

	boolean isSubscriber(WorkspaceMember member) {
		return subscribers.stream()
			.anyMatch(s -> s.getSubscriber().equals(member));
	}

}
