package com.tissue.issue.domain;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.issue.domain.exception.IssueErrorCode.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.exception.ContextKeys;
import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.sprint.domain.Sprint;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.enums.StateCategory;

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sprint_id")
	private Sprint sprint;

	@ManyToOne(fetch = FetchType.LAZY)
	private IssueType issueType;

	// TODO: issueType로 부터 얻은 issueHierarchy를 편의 필드로 둘까?
	//  - 아니면 항상 issue를 조회할때 issueType도 join fetch로 가져오도록 할까?
	//  - 그런데 이렇게 설계할거면 이슈에 대한 issueType는 절대로 변하지 않을거라는 정책을 사용해야 함
	//   다른 플랫폼에서도 이렇게 하나?

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowState currentState;

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueFieldValue> fieldValues = new ArrayList<>();

	// TODO: 추후 태그(tag) 추가. 분류와 검색용도로 활용. 일단은 보류.

	public static Issue create(
		@NonNull Project project,
		@Nullable Sprint sprint,
		@NonNull IssueType issueType,
		@NonNull String title,
		@NonNull IssueContent content,
		@NonNull IssueSchedule schedule,
		@NonNull IssueParticipants participants,
		@Nullable IssuePriority priority,
		@Nullable Integer storyPoint,
		@Nullable Issue parentIssue
	) {
		Issue issue = new Issue();
		issue.project = project;
		issue.projectKey = project.getKey();
		issue.workspaceKey = project.getWorkspaceKey();

		issue.sprint = sprint;

		issue.key = project.generateNextIssueKey();
		issue.issueType = issueType;
		issue.title = title;
		issue.content = content;
		issue.schedule = schedule;
		issue.participants = participants;
		issue.priority = priority == null ? IssuePriority.NORMAL : priority;

		if (storyPoint != null) {
			issue.updateStoryPoint(storyPoint);
		}

		issue.progress = IssueProgress.init();
		issue.relations = IssueRelations.init();

		if (parentIssue != null) {
			issue.setParentIssue(parentIssue);
		}

		return issue;
	}

	public String getContent() {
		return content.getContent();
	}

	public String getSummary() {
		return content.getSummary();
	}

	public List<IssueFieldValue> getFieldValues() {
		return Collections.unmodifiableList(fieldValues);
	}

	public IssueFieldValue addOrUpdateFieldValue(IssueField field) {
		return this.fieldValues.stream()
			.filter(fv -> fv.getField().equals(field))
			.findFirst()
			.orElseGet(() -> {
				IssueFieldValue newValue = IssueFieldValue.of(this, field);
				this.fieldValues.add(newValue);
				return newValue;
			});
	}

	public void setSprint(@NonNull Sprint sprint) {
		this.sprint = sprint;
	}

	public void clearSprint() {
		this.sprint = null;
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
		ensureCanModifyStoryPoint();
		this.storyPoint = storyPoint;
	}

	public void recalculateEpicStoryPoint(int totalChildrenStoryPoints) {
		if (getHierarchy().isNotEpic()) {
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

	public IssueRelation removeRelation(@NonNull Issue otherIssue) {
		return relations.removeRelation(otherIssue);
	}

	// TODO: 도메인 서비스로 분리할까?
	public void transitionTo(@NonNull WorkflowState newState) {
		WorkflowState previousState = this.currentState;
		this.currentState = newState;

		if (previousState.getCategory().isTodo()) {
			this.schedule.markStarted();
		}
		if (newState.getCategory().isDone()) {
			this.schedule.markResolved();
		}
		if (previousState.getCategory().isDone() && !newState.getCategory().isDone()) {
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
		this.parentIssue = newParent;
	}

	public void removeParentIssue() {
		ensureCanRemoveParent();
		parentIssue = null;
	}

	public void delete() {
		ensureIsInitial();
		softDelete();
	}

	public IssueHierarchy getHierarchy() {
		return issueType.getIssueHierarchy();
	}

	public int getSubscribersCount() {
		return participants.getSubscribers().size();
	}

	private void ensureIsInitial() {
		if (!currentState.isCategorizedAs(StateCategory.TODO)) {
			throw new BadRequestException(ONLY_INITIAL_STATE_DELETION_ALLOWED)
				.addContext(ContextKeys.WORKSPACE_KEY, this.getWorkspaceKey())
				.addContext(ContextKeys.ISSUE_KEY, this.getKey())
				.addContext(ContextKeys.CURRENT_STATE, this.getCurrentState().getDisplayLabel())
				.addContext(ContextKeys.STATE_CATEGORY, this.getCurrentState().getCategory());
		}
	}

	private void ensureCanModifyStoryPoint() {
		if (this.getHierarchy().cannotModifyStoryPoint()) {
			throw new BadRequestException(STORY_POINT_NOT_ALLOWED)
				.addContext(WORKSPACE_KEY, this.getWorkspaceKey())
				.addContext(ISSUE_KEY, this.getKey())
				.addContext(CURRENT_HIERARCHY, this.getHierarchy())
				.addContext(STORY_POINT_ALLOWED_HIERARCHIES, IssueHierarchy.getStoryPointModifiable());
		}
	}

	private void ensureCanSetParent(@NonNull Issue parentIssue) {
		ensureSameWorkspace(parentIssue);
		if (this.getHierarchy().cannotHaveCrossProjectParent()) {
			ensureSameProject(parentIssue);
		}
		ensureNotSelfReference(parentIssue);
		ensureValidParentHierarchy(parentIssue);
	}

	private void ensureValidParentHierarchy(Issue parentIssue) {
		IssueHierarchy parentHierarchy = parentIssue.getHierarchy();
		IssueHierarchy childHierarchy = this.getHierarchy();

		if (parentHierarchy.cannotBeParentOf(childHierarchy)) {
			throw IssueExceptions.invalidParentHierarchy(
				this.getWorkspaceKey(),
				parentIssue.getKey(),
				parentHierarchy,
				this.getKey(),
				childHierarchy
			);
		}
	}

	private void ensureNotSelfReference(Issue parentIssue) {
		if (this.equals(parentIssue)) {
			throw new BadRequestException(ISSUE_SELF_REFERENCE)
				.addContext(WORKSPACE_KEY, this.getWorkspaceKey())
				.addContext(ISSUE_KEY, this.getKey());
		}
	}

	private void ensureSameWorkspace(Issue parentIssue) {
		boolean isDifferentWorkspace = !this.getWorkspaceKey().equals(parentIssue.getWorkspaceKey());
		if (isDifferentWorkspace) {
			throw new BadRequestException(PARENT_WORKSPACE_MISMATCH)
				.addContext(PARENT_WORKSPACE_KEY, parentIssue.getWorkspaceKey())
				.addContext(PARENT_ISSUE_KEY, parentIssue.getKey())
				.addContext(CHILD_WORKSPACE_KEY, this.getWorkspaceKey())
				.addContext(CHILD_ISSUE_KEY, this.getKey());
		}
	}

	private void ensureSameProject(Issue parentIssue) {
		boolean isDifferentProject = !this.getProjectKey().equals(parentIssue.getProjectKey());
		if (isDifferentProject) {
			throw new BadRequestException(PARENT_PROJECT_MISMATCH)
				.addContext(PARENT_HIERARCHY, parentIssue.getHierarchy())
				.addContext(PARENT_ISSUE_KEY, parentIssue.getKey())
				.addContext(CHILD_HIERARCHY, this.getHierarchy())
				.addContext(CHILD_ISSUE_KEY, this.getKey());
		}
	}

	private void ensureCanRemoveParent() {
		if (getHierarchy().mustHaveParent()) {
			throw new BadRequestException(PARENT_REQUIRED)
				.addContext(WORKSPACE_KEY, this.getWorkspaceKey())
				.addContext(ISSUE_KEY, this.getKey())
				.addContext(CURRENT_HIERARCHY, this.getHierarchy())
				.addContext(HIERARCHIES_REQUIRING_PARENT, IssueHierarchy.getParentRequired());
		}
	}

	@Override
	public String toString() {
		return "Issue{id=%d, key='%s', project='%s', workspace='%s', title='%s'}"
			.formatted(id, key, projectKey, workspaceKey, title);
	}
}
