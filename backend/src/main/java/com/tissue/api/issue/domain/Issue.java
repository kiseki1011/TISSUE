package com.tissue.api.issue.domain;

import java.time.Instant;
import java.util.Objects;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.issue.exception.InvalidParentHierarchyException;
import com.tissue.api.issue.exception.IssueSelfReferenceException;
import com.tissue.api.issue.exception.ParentRequiredException;
import com.tissue.api.issue.exception.ParentWorkspaceMismatchException;
import com.tissue.api.issue.exception.StoryPointNotAllowedForHierarchyException;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

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

	@Column(name = "workspace_key", nullable = false, updatable = false)
	private String workspaceKey;

	// TODO: project 애그리거트 추가 후 추가
	// @Column(name = "project_key", nullable = false, updatable = false)
	// private String projectKey;

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

	// TODO: 추후 Sprint 쪽 애그리거트 정리 후, 다시 리팩토링 진행. 일단은 보류.
	// @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	// private Set<SprintIssue> sprintIssues = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private IssueType issueType;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowState currentState;

	// TODO: 추후 태그(tag) 추가. 분류와 검색용도로 활용. 일단은 보류.

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
		// issue.projectKey = project.getKey();
		// issue.workspaceKey = project.getWorkspaceKey();
		issue.key = workspace.generateCurrentIssueKey();
		issue.issueType = issueType;
		issue.title = title;
		issue.content = content;
		issue.schedule = schedule;
		issue.participants = participants;
		issue.priority = setDefaultPriorityIfNull(priority);
		issue.storyPoint = ensureCanModifyStoryPoint(issue.getHierarchy(), storyPoint);

		issue.progress = IssueProgress.init();
		issue.relations = IssueRelations.init();

		return issue;
	}

	public void changeReporter(@NonNull WorkspaceMember reporter) {
		if (participants.isReporter(reporter)) {
			return;
		}
		participants.changeReporter(reporter);
	}

	public void updateTitle(@NonNull String title) {
		this.title = title;
	}

	public void updateContent(@Nullable String content) {
		this.content.updateContent(content);
	}

	public void updateSummary(@Nullable String summary) {
		content.updateSummary(summary);
	}

	public void updateDueAt(@Nullable Instant dueAt) {
		schedule.updateDueDate(dueAt);
	}

	public void updatePriority(@NonNull IssuePriority priority) {
		this.priority = priority;
	}

	public void updateStoryPoint(@Nullable Integer storyPoint) {
		ensureCanModifyStoryPoint(this.getHierarchy(), storyPoint);
		this.storyPoint = storyPoint;
	}

	public void recalculateEpicStoryPoint(int totalChildrenStoryPoints) {
		if (isNotEpic()) {
			return;
		}
		this.storyPoint = totalChildrenStoryPoints;
	}

	public void updateProgress(@Nullable Integer countBased, @Nullable Integer pointBased) {
		progress.update(countBased, pointBased);
	}

	public IssueRelation addRelation(@NonNull Issue targetIssue, @NonNull IssueRelationType type) {
		return relations.addRelation(this, targetIssue, type);
	}

	public void removeRelation(@NonNull Issue otherIssue) {
		relations.removeRelation(otherIssue);
	}

	public void transitionTo(@NonNull WorkflowState newState) {
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
		participants.addSubscriber(workspaceMember, this);
	}

	public void removeSubscriber(@NonNull WorkspaceMember workspaceMember) {
		participants.removeSubscriber(workspaceMember);
	}

	public void assignTo(@NonNull WorkspaceMember assignee) {
		participants.assignTo(assignee);
	}

	public void unassign() {
		participants.unassign();
	}

	public void addReviewer(@NonNull WorkspaceMember workspaceMember) {
		participants.addReviewer(workspaceMember, this);
	}

	public void removeReviewer(@NonNull WorkspaceMember workspaceMember) {
		participants.removeReviewer(workspaceMember);
	}

	public void setParentIssue(@NonNull Issue newParent) {
		ensureCanSetParent(newParent);
		clearParent();

		this.parentIssue = newParent;
	}

	public void removeParentIssue() {
		ensureCanRemoveParent();
		clearParent();
	}

	public void softDelete() {
		ensureIsInitial();
		clearParticipants();
		clearRelations();
		clearParent();
		archive();
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}

	public boolean isNotEpic() {
		return issueType.getIssueHierarchy() != IssueHierarchy.EPIC;
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

	public int getSubscribersCount() {
		return participants.getSubscribers().size();
	}

	public boolean isParticipant(@NonNull WorkspaceMember wm) {
		return isAuthor(wm.getMemberId()) ||
			participants.isReporter(wm) ||
			participants.isAssignee(wm) ||
			participants.isReviewer(wm) ||
			participants.isSubscriber(wm);
	}

	private void ensureIsInitial() {
		if (!currentState.isInitial()) {
			throw new RuntimeException("Cannot delete issue that is not initial state.");
		}
	}

	private static Integer ensureCanModifyStoryPoint(IssueHierarchy hierarchy, Integer storyPoint) {
		if (hierarchy.cannotModifyStoryPoint()) {
			throw new StoryPointNotAllowedForHierarchyException(hierarchy);
		}
		return storyPoint;
	}

	private static IssuePriority setDefaultPriorityIfNull(IssuePriority priority) {
		return priority == null ? IssuePriority.NORMAL : priority;
	}

	private void clearParent() {
		parentIssue = null;

	}

	private void ensureCanSetParent(@NonNull Issue parentIssue) {
		ensureSameWorkspace(parentIssue);
		// TODO: ensureSameProject - SUB_TASK 이하인 경우
		ensureNotSelfReference(parentIssue);
		ensureValidParentHierarchy(parentIssue);
	}

	private void ensureValidParentHierarchy(Issue parentIssue) {
		IssueHierarchy parentHierarchy = parentIssue.getHierarchy();
		IssueHierarchy childHierarchy = this.getHierarchy();

		if (parentHierarchy.cannotBeParentOf(childHierarchy)) {
			throw new InvalidParentHierarchyException(parentIssue.getKey(), parentHierarchy, this.key, childHierarchy);
		}
	}

	private void ensureNotSelfReference(Issue parentIssue) {
		if (this.equals(parentIssue)) {
			throw new IssueSelfReferenceException(this.key);
		}
	}

	private void ensureSameWorkspace(Issue parentIssue) {
		boolean isDifferentWorkspace = !this.getWorkspace().equals(parentIssue.getWorkspace());
		if (isDifferentWorkspace) {
			throw new ParentWorkspaceMismatchException(
				parentIssue.workspace.getKey(),
				parentIssue.key,
				this.workspace.getKey(),
				this.key
			);
		}
	}

	// TODO: ensureSameProject - STORY 이하의 hierarchy를 가지는 경우 자식은 무조건 같은 Project 내에서만 할 수 있도록 허용
	//  - EPIC과 STORY 사이의 경우에는 cross-project 허용

	private void ensureCanRemoveParent() {
		if (getHierarchy().mustHaveParent()) {
			throw new ParentRequiredException(this.key, getHierarchy().toString());
		}
	}

	private void clearParticipants() {
		participants.clear();
	}

	private void clearRelations() {
		relations.clear();
	}

	@Override
	public String toString() {
		return "Issue{id=%d, key='%s', workspace='%s', title='%s'}"
			.formatted(id, key, getWorkspaceKey(), title);
	}
}
