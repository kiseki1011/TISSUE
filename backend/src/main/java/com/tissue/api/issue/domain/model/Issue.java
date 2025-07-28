package com.tissue.api.issue.domain.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.model.enums.IssuePriority;
import com.tissue.api.issue.domain.newmodel.IssueTypeDefinition;
import com.tissue.api.issue.domain.newmodel.JsonMapConverter;
import com.tissue.api.issue.domain.newmodel.WorkflowStep;
import com.tissue.api.sprint.domain.model.SprintIssue;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Have I set the UniqueConstraint properly?
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workspaceCode", "issueKey"})
})
@EqualsAndHashCode(of = {"issueKey", "workspaceCode"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends BaseEntity {

	// TODO: should i consider reading the value from application.yml?
	private static final int MAX_REVIEWERS = 10;
	private static final int MAX_ASSIGNEES = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// issue key must be unique for each workspace
	@Column(nullable = false)
	private String issueKey;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(nullable = false)
	private String workspaceCode;

	@Column(nullable = false)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	@Lob
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssuePriority priority;

	private LocalDateTime startedAt;
	private LocalDateTime resolvedAt;

	@Column(nullable = false)
	private LocalDateTime dueAt;

	private Integer storyPoint;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_issue_id")
	private Issue parentIssue;

	@OneToMany(mappedBy = "parentIssue")
	private Set<Issue> childIssues = new HashSet<>();

	@OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> outgoingRelations = new HashSet<>();

	@OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> incomingRelations = new HashSet<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueReviewer> reviewers = new HashSet<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueAssignee> assignees = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "issue_id")
	private Set<IssueWatcher> watchers = new HashSet<>();

	@OneToMany(mappedBy = "issue")
	private Set<SprintIssue> sprintIssues = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private IssueTypeDefinition issueType;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep currentStep;

	@Column(columnDefinition = "json")
	@Convert(converter = JsonMapConverter.class)
	private Map<String, Object> customFields = new HashMap<>();

	@Builder
	protected Issue(
		Workspace workspace,
		IssueTypeDefinition issueType,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDateTime dueAt,
		Integer storyPoint
	) {
		this.issueKey = workspace.getIssueKey();
		workspace.increaseNextIssueNumber();

		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();

		// TODO: Should I use a uni-directional jpa relation between Workspace and Issue,
		//  instead of a bi-directional relation?
		// TODO: If using uni-directional relation, how should I be able to retrieve issues that belong to
		//  a specific workspace?
		workspace.getIssues().add(this);

		this.title = title;
		this.content = content;
		this.summary = summary;
		this.priority = priority != null ? priority : IssuePriority.MEDIUM;
		this.dueAt = dueAt;
		this.storyPoint = storyPoint;
		this.issueType = issueType;
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public void updateSummary(String summary) {
		this.summary = summary;
	}

	public void updateDueAt(LocalDateTime dueAt) {
		this.dueAt = dueAt;
	}

	public boolean hasParent() {
		return parentIssue != null;
	}

	public void updatePriority(IssuePriority priority) {
		this.priority = priority;
	}

	public void updateParentIssue(Issue parentIssue) {
		validateParentIssue(parentIssue);
		removeParentRelationship();

		this.parentIssue = parentIssue;
		parentIssue.getChildIssues().add(this);
	}

	public void removeParentRelationship() {
		if (parentIssue != null) {
			parentIssue.getChildIssues().remove(this);
			parentIssue = null;
		}
	}

	public void validateParentIssue(Issue parentIssue) {
	}

	public void validateCanRemoveParent() {
	}

	// TODO: How should I define the logic to automatically update the timestamps
	//  when the currentStep is a finalStep?
	// private void updateTimestamps(WorkflowStep newStep) {
	// }

	public void updateStoryPoint(Integer storyPoint) {
		this.storyPoint = storyPoint;
	}

	public boolean isAuthor(Long memberId) {
		return Objects.equals(getCreatedBy(), memberId);
	}

	public void addWatcher(WorkspaceMember workspaceMember) {
		IssueWatcher watcher = new IssueWatcher(workspaceMember);
		watchers.add(watcher);
	}

	public void removeWatcher(WorkspaceMember workspaceMember) {
		watchers.removeIf(watcher -> watcher.getWatcher().equals(workspaceMember));
	}

	public Set<Long> getSubscriberMemberIds() {
		Set<Long> memberIds = new HashSet<>();

		// add memberId of author
		if (this.getCreatedBy() != null) {
			memberIds.add(this.getCreatedBy());
		}

		// add Assignee memberIds
		assignees.stream()
			.map(IssueAssignee::getAssigneeMemberId)
			.forEach(memberIds::add);

		// add Reviewer memberIds
		reviewers.stream()
			.map(IssueReviewer::getReviewerMemberId)
			.forEach(memberIds::add);

		// add Watcher memberIds
		watchers.stream()
			.map(IssueWatcher::getWatcherMemberId)
			.forEach(memberIds::add);

		return memberIds;
	}

	public IssueAssignee addAssignee(WorkspaceMember workspaceMember) {
		validateAssigneeLimit();
		validateBelongsToWorkspace(workspaceMember);

		IssueAssignee assignee = new IssueAssignee(this, workspaceMember);

		assignees.add(assignee);
		return assignee;
	}

	public void removeAssignee(WorkspaceMember assignee) {
		IssueAssignee issueAssignee = findIssueAssignee(assignee);
		assignees.remove(issueAssignee);
	}

	public void validateIsAssignee(Long memberId) {
		boolean isNotAssignee = !isAssignee(memberId);

		if (isNotAssignee) {
			throw new ForbiddenOperationException(
				String.format("Must be an assignee of this issue. workspace code: %s, issue key: %s, member id: %d",
					workspaceCode, issueKey, memberId));
		}
	}

	private IssueAssignee findIssueAssignee(WorkspaceMember assignee) {
		return assignees.stream()
			.filter(ia -> ia.getAssignee().getId().equals(assignee.getId()))
			.findFirst()
			.orElseThrow(() -> new InvalidOperationException(
				String.format("Is not a assignee assigned to this issue. workspaceMemberId: %d, displayName: %s",
					assignee.getId(), assignee.getDisplayName()))
			);
	}

	private boolean isAssignee(Long memberId) {
		return assignees.stream()
			.anyMatch(issueAssignee -> Objects.equals(issueAssignee.getAssigneeMemberId(), memberId));
	}

	private void validateAssigneeLimit() {
		if (assignees.size() >= MAX_ASSIGNEES) {
			throw new InvalidOperationException(
				String.format("The maximum number of assignees for a single issue is %d", MAX_ASSIGNEES));
		}
	}

	private void validateBelongsToWorkspace(WorkspaceMember workspaceMember) {
		boolean hasDifferentWorkspaceCode = !workspaceMember.getWorkspaceCode().equals(workspaceCode);

		if (hasDifferentWorkspaceCode) {
			throw new InvalidOperationException(String.format(
				"Assignee must belong to this workspace. expected: %s , actual: %s",
				workspaceMember.getWorkspaceCode(), workspaceCode));
		}
	}

	public Set<Long> getReviewerMemberIds() {
		Set<Long> reviewerIds = new HashSet<>();

		reviewers.stream()
			.map(IssueReviewer::getReviewerMemberId)
			.forEach(reviewerIds::add);

		return reviewerIds;
	}

	public IssueReviewer addReviewer(WorkspaceMember workspaceMember) {
		validateReviewerLimit();
		validateIsReviewer(workspaceMember);

		IssueReviewer reviewer = new IssueReviewer(workspaceMember, this);
		reviewers.add(reviewer);

		return reviewer;
	}

	public void removeReviewer(WorkspaceMember workspaceMember) {
		IssueReviewer issueReviewer = findIssueReviewer(workspaceMember);
		reviewers.remove(issueReviewer);
	}

	private void validateReviewerLimit() {
		if (reviewers.size() >= MAX_REVIEWERS) {
			throw new InvalidOperationException(
				String.format("The max number of reviewers for a single issue is %d.", MAX_REVIEWERS));
		}
	}

	private void validateIsReviewer(WorkspaceMember workspaceMember) {
		boolean isAlreadyReviewer = reviewers.stream()
			.anyMatch(r -> r.getReviewer().getId().equals(workspaceMember.getId()));

		if (isAlreadyReviewer) {
			throw new InvalidOperationException(
				String.format("Workspace member is already a reviewer. workspaceMemberId: %d",
					workspaceMember.getId()));
		}
	}

	private IssueReviewer findIssueReviewer(WorkspaceMember workspaceMember) {
		return reviewers.stream()
			.filter(r -> r.getReviewer().getId().equals(workspaceMember.getId()))
			.findFirst()
			.orElseThrow(() -> new ForbiddenOperationException(
				String.format("Not a reviewer assigned to this issue. workspaceMemberId: %d, displayName: %s",
					workspaceMember.getId(), workspaceMember.getDisplayName()))
			);
	}

	// TODO: IssueRelation related codes need to be modified after implementing
	//  custom IssueTypeDefinition, WorkflowDefinition, etc...
	//  Lets do this later.
	// private void validateBlockingIssuesAreDone() {
	// 	List<com.tissue.api.issue.domain.newmodel.Issue> blockingIssues = incomingRelations.stream()
	// 		.filter(relation -> relation.getRelationType() == IssueRelationType.BLOCKED_BY)
	// 		.map(IssueRelation::getSourceIssue)
	// 		.filter(issue -> issue.getStatus() != DONE)
	// 		.toList();
	//
	// 	if (!blockingIssues.isEmpty()) {
	// 		String blockingIssueKeys = blockingIssues.stream()
	// 			.map(com.tissue.api.issue.domain.newmodel.Issue::getIssueKey)
	// 			.collect(Collectors.joining(", "));
	//
	// 		throw new InvalidOperationException(
	// 			String.format("Cannot complete this issue. Blocking issues must be completed first: %s",
	// 				blockingIssueKeys));
	// 	}
	// }
}
