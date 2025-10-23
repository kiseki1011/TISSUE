package com.tissue.api.issue.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.issue.domain.model.vo.IssueContent;
import com.tissue.api.issue.domain.model.vo.IssueParticipants;
import com.tissue.api.issue.domain.model.vo.IssueProgress;
import com.tissue.api.issue.domain.model.vo.IssueSchedule;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.sprint.domain.model.SprintIssue;
import com.tissue.api.workflow.domain.model.WorkflowState;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@SQLRestriction("archived = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "issue_key", nullable = false)
	private String key;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@Column(name = "title", nullable = false)
	private String title;

	@Embedded
	private IssueContent content;

	@Embedded
	private IssueSchedule schedule;

	@Embedded
	private IssueProgress progress;

	@Embedded
	private IssueParticipants participants;

	@Embedded
	private IssueRelations relations;

	@Enumerated(EnumType.STRING)
	@Column(name = "priority", nullable = false)
	private IssuePriority priority;

	@Column(name = "story_point")
	private Integer storyPoint;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_issue_id")
	private Issue parentIssue;

	@OneToMany(mappedBy = "parentIssue")
	private List<Issue> childIssues = new ArrayList<>();

	// TODO: 리팩토링 필요 -> 더 좋은 이름, Sprint와의 관계 구조 개선
	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<SprintIssue> sprintIssues = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private IssueType issueType;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowState currentState;

	// TODO: 태그(tag) 추가. 분류와 검색용도로 활용

	public static Issue create(
		@NonNull Workspace workspace,
		@NonNull IssueType issueType,
		@NonNull String title,
		@NonNull IssueContent content,
		@NonNull IssueSchedule schedule,
		@NonNull IssueParticipants participants,
		@Nullable IssuePriority priority,
		@Nullable Integer storyPoint
	) {
		Issue issue = new Issue();
		issue.workspace = workspace;
		issue.issueType = issueType;
		issue.title = title;
		issue.content = content;
		issue.schedule = schedule;
		issue.participants = participants;
		issue.priority = defaultPriorityIfNull(priority);
		issue.storyPoint = ensureCanUseStoryPoint(issue.getHierarchy(), storyPoint);

		issue.progress = IssueProgress.init();
		issue.relations = IssueRelations.init();

		return issue;
	}

	public void changeReporter(@NonNull WorkspaceMember reporter) {
		this.participants.changeReporter(reporter);
	}

	public void updateTitle(@NonNull String title) {
		this.title = title;
	}

	public void updateContent(@Nullable String content) {
		this.content.updateContent(content);
	}

	public void updateSummary(@Nullable String summary) {
		this.content.updateSummary(summary);
	}

	public void updateDueAt(@Nullable Instant dueAt) {
		this.schedule.updateDueDate(dueAt);
	}

	public void updatePriority(@NonNull IssuePriority priority) {
		this.priority = priority;
	}

	public void updateStoryPoint(@Nullable Integer storyPoint) {
		if (storyPoint != null) {
			ensureCanUseStoryPoint(this.getHierarchy(), storyPoint);
		}
		this.storyPoint = storyPoint;
	}

	public void recalculateEpicStoryPoint() {
		if (getHierarchy() != IssueHierarchy.EPIC) {
			return;
		}
		this.storyPoint = this.getChildIssues().stream()
			.filter(child -> child.getStoryPoint() != null)
			.mapToInt(Issue::getStoryPoint)
			.sum();
	}

	public void updateProgress(@Nullable Integer countBased, @Nullable Integer pointBased) {
		this.progress.update(countBased, pointBased);
	}

	public IssueRelation addRelation(@NonNull Issue targetIssue, @NonNull IssueRelationType type) {
		return this.relations.addRelation(this, targetIssue, type);
	}

	public void removeRelation(@NonNull Issue otherIssue) {
		this.relations.removeRelation(this, otherIssue);
	}

	public void proceedToNextState(@NonNull WorkflowState newState) {
		WorkflowState previousState = this.currentState;
		this.currentState = newState;

		if (previousState.isInitial()) {
			this.schedule.markStarted();
		}
		if (newState.isTerminal()) {
			this.schedule.markResolved();
		}
		if (previousState.isTerminal() && !newState.isTerminal()) {
			this.schedule.clearResolved();
		}
	}

	public void addSubscriber(@NonNull WorkspaceMember workspaceMember) {
		this.participants.addSubscriber(workspaceMember, this);
	}

	public void removeSubscriber(@NonNull WorkspaceMember workspaceMember) {
		this.participants.removeSubscriber(workspaceMember);
	}

	public void assignTo(@NonNull WorkspaceMember assignee) {
		this.participants.assignTo(assignee);
	}

	public void unassign() {
		this.participants.unassign();
	}

	public void addReviewer(@NonNull WorkspaceMember workspaceMember) {
		this.participants.addReviewer(workspaceMember, this);
	}

	public void removeReviewer(@NonNull WorkspaceMember workspaceMember) {
		this.participants.removeReviewer(workspaceMember);
	}

	public void setParentIssue(@NonNull Issue newParent) {
		ensureCanSetParent(newParent);
		detachFromCurrentParent();

		this.parentIssue = newParent;
		newParent.childIssues.add(this);
	}

	public void removeParentIssue() {
		ensureCanRemoveParent();
		detachFromCurrentParent();
	}

	public void softDelete() {
		ensureDeletable();
		clearParticipants();
		clearRelations();
		detachFromCurrentParent();
		archive();
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}

	public IssueHierarchy getHierarchy() {
		return issueType.getIssueHierarchy();
	}

	public boolean isDone() {
		return currentState.getCategory() == StateCategory.DONE;
	}

	public boolean isAuthor(@NonNull Long memberId) {
		return Objects.equals(getCreatedBy(), memberId);
	}

	public boolean isParticipant(@NonNull WorkspaceMember wm) {
		return isAuthor(wm.getMemberId()) ||
			participants.isReporter(wm) ||
			participants.isAssignee(wm) ||
			participants.isReviewer(wm) ||
			participants.isSubscriber(wm);
	}

	private void ensureDeletable() {
		if (!currentState.isInitial()) {
			throw new RuntimeException("Cannot delete issue that is not initial state.");
		}
		if (!childIssues.isEmpty()) {
			throw new RuntimeException("Cannot delete issue that has children.");
		}
	}

	private static Integer ensureCanUseStoryPoint(IssueHierarchy hierarchy, Integer storyPoint) {
		if (storyPoint == null) {
			return null;
		}
		if (hierarchy.cannotHaveStoryPoint()) {
			throw new InvalidOperationException("Cannot set story point for hierarchy: " + hierarchy);
		}
		return storyPoint;
	}

	private static IssuePriority defaultPriorityIfNull(IssuePriority priority) {
		return priority == null ? IssuePriority.NORMAL : priority;
	}

	private void detachFromCurrentParent() {
		if (parentIssue != null) {
			parentIssue.getChildIssues().remove(this);
			parentIssue = null;
		}
	}

	private void ensureCanSetParent(@NonNull Issue parentIssue) {
		ensureSameWorkspace(parentIssue);
		ensureNotSelfReference(parentIssue);
		ensureValidHierarchy(parentIssue);
	}

	private void ensureValidHierarchy(Issue parentIssue) {
		IssueHierarchy parentHierarchy = parentIssue.getHierarchy();
		IssueHierarchy childHierarchy = this.getHierarchy();

		if (!parentHierarchy.canBeParentOf(childHierarchy)) {
			throw new InvalidOperationException(
				"Parent must be exactly one level above the child. Parent: %s (%s), Child: %s (%s)"
					.formatted(parentIssue.getIssueType().getLabel(), parentHierarchy, this.issueType.getLabel(),
						childHierarchy));
		}
	}

	private void ensureNotSelfReference(Issue parentIssue) {
		if (this.equals(parentIssue)) {
			throw new InvalidOperationException("An issue cannot be its own parent.");
		}
	}

	private void ensureSameWorkspace(Issue parentIssue) {
		boolean isDifferentWorkspace = !this.getWorkspace().equals(parentIssue.getWorkspace());
		if (isDifferentWorkspace) {
			throw new InvalidOperationException("Parent must belong to the same workspace.");
		}
	}

	private void ensureCanRemoveParent() {
		if (getHierarchy().mustHaveParent()) {
			throw new RuntimeException("Issues at SUBTASK or MICROTASK level must have a parent. Cannot stand alone.");
		}
	}

	private void clearParticipants() {
		participants.clear();
	}

	private void clearRelations() {
		this.relations.clear();
	}

	@Override
	public String toString() {
		return "Issue{id=%d, key='%s', workspace='%s', title='%s'}"
			.formatted(id, key, getWorkspaceKey(), title);
	}
}
