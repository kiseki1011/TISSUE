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
import com.tissue.api.issue.domain.exception.InvalidParentHierarchyException;
import com.tissue.api.issue.domain.exception.IssueSelfReferenceException;
import com.tissue.api.issue.domain.exception.ParentRequiredException;
import com.tissue.api.issue.domain.exception.ParentWorkspaceMismatchException;
import com.tissue.api.issue.domain.exception.StoryPointNotAllowedForHierarchyException;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.ProjectMember;
import com.tissue.api.workflow.domain.WorkflowState;

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
@SQLRestriction("softDeleted = false")
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
	private Project project;

	@Column(name = "project_key", nullable = false, updatable = false)
	private String projectKey;

	@Column(name = "workspace_key", nullable = false, updatable = false)
	private String workspaceKey;

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
		@NonNull Project project,
		@NonNull IssueType issueType,
		@NonNull String title,
		@NonNull IssueContent content,
		@NonNull IssueSchedule schedule,
		@NonNull IssueParticipants participants,
		@Nullable IssuePriority priority,
		@Nullable Integer storyPoint
	) {
		Issue issue = new Issue();
		issue.project = project;
		issue.projectKey = project.getKey();
		issue.workspaceKey = project.getWorkspaceKey();

		issue.key = project.generateNextIssueKey();
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

	public void changeReporter(@NonNull ProjectMember reporter) {
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

	public void addSubscriber(@NonNull ProjectMember projectMember) {
		participants.addSubscriber(projectMember, this);
	}

	public void removeSubscriber(@NonNull ProjectMember projectMember) {
		participants.removeSubscriber(projectMember);
	}

	public void assignTo(@NonNull ProjectMember assignee) {
		participants.assignTo(assignee);
	}

	public void unassign() {
		participants.unassign();
	}

	public void addReviewer(@NonNull ProjectMember projectMember) {
		participants.addReviewer(projectMember, this);
	}

	public void removeReviewer(@NonNull ProjectMember projectMember) {
		participants.removeReviewer(projectMember);
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

	public void delete() {
		ensureIsInitial();
		clearParticipants();
		clearRelations();
		clearParent();
		softDelete();
	}

	public String getWorkspaceKey() {
		return project.getKey();
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

	public boolean isParticipant(@NonNull ProjectMember pm) {
		return isAuthor(pm.getMemberId()) ||
			participants.isReporter(pm) ||
			participants.isAssignee(pm) ||
			participants.isReviewer(pm) ||
			participants.isSubscriber(pm);
	}

	private void ensureIsInitial() {
		if (!currentState.isInitial()) {
			// TODO: InProgressIssueNotDeletable (이름 피드백 필요)
			throw new RuntimeException("Cannot delete issue that is not initial state.");
		}
	}

	private static Integer ensureCanModifyStoryPoint(IssueHierarchy hierarchy, Integer storyPoint) {
		if (hierarchy.cannotModifyStoryPoint()) {
			// TODO: 예외 이름 개선
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
		if (this.getHierarchy().cannotHaveCrossProjectChild()) {
			ensureSameProject(parentIssue);
		}
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
		boolean isDifferentWorkspace = !this.getWorkspaceKey().equals(parentIssue.getWorkspaceKey());
		if (isDifferentWorkspace) {
			throw new ParentWorkspaceMismatchException(
				parentIssue.getWorkspaceKey(),
				parentIssue.key,
				this.getWorkspaceKey(),
				this.key
			);
		}
	}

	private void ensureSameProject(Issue parentIssue) {
		boolean isDifferentProject = !this.getProject().equals(parentIssue.getProject());
		if (isDifferentProject) {
			throw new RuntimeException("Children of issues below EPIC level must be in the same project.");
			// TODO:
			// throw new ProjectMismatchException(
			// 	parentIssue.getWorkspaceKey(),
			// 	parentIssue.key,
			// 	this.getWorkspaceKey(),
			// 	this.key
			// );
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
		return "Issue{id=%d, key='%s', project='%s', workspace='%s', title='%s'}"
			.formatted(id, key, projectKey, getWorkspaceKey(), title);
	}
}
