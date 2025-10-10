package com.tissue.api.issue.domain.model;

import static com.tissue.api.issue.domain.enums.IssueHierarchy.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.sprint.domain.model.SprintIssue;
import com.tissue.api.workflow.domain.model.WorkflowStatus;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends BaseEntity {

	// TODO: use application.yml for value
	private static final int MAX_REVIEWERS = 10;
	private static final int MAX_ASSIGNEES = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "issue_key", nullable = false)
	private String key;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private WorkspaceMember reporter;

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

	// TODO: must be set on workflow transition from intial to next step
	private Instant startedAt;

	// TODO: must be set when workflow status reaches terminal
	private Instant resolvedAt;

	@Column(nullable = false)
	private Instant dueAt;

	private Integer storyPoint;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_issue_id")
	private Issue parentIssue;

	// TODO: Set vs List? which is better?
	@OneToMany(mappedBy = "parentIssue")
	private Set<Issue> childIssues = new HashSet<>();

	@OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> outgoingRelations = new HashSet<>();

	@OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> incomingRelations = new HashSet<>();

	// TODO: IssueReviewer, IssueAssignee, IssueWatcher를 Issue 와 WorkspaceMember 사이의 중간 엔티티로 설계하는게 좋은 방법인걸까?
	//  @ManyToMany는 비권장하기 때문에 중간 엔티티를 통해 다대다 관계를 형성하긴 했지만, 이게 좋은 방법인지는 모름.
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
	private IssueType issueType;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus currentStatus;

	public static Issue create(
		@NonNull Workspace workspace,
		@NonNull IssueType issueType,
		@NonNull String title,
		@Nullable String content,
		@Nullable String summary,
		IssuePriority priority, // TODO: nullable or non-null?
		@Nullable Instant dueAt,
		@Nullable Integer storyPoint
	) {
		Issue issue = new Issue();
		issue.workspace = workspace;
		issue.issueType = issueType;
		issue.title = title;
		issue.content = content;
		issue.summary = summary;
		issue.priority = priority;
		issue.dueAt = dueAt;
		// TODO: IssueType의 Hierarchy가 Hierarchy.EPIC, Hierarchy.SUBTASK, Hierarchy.MICROTASK면 값 설정 금지(null)
		//  - 로직이 ensureCanUseStoryPoint()과 유사한데, static 메서드 내에서는 ensureCanUseStoryPoint를 사용못함. 좋은 방법 없을까?
		//  - 아니면 이런 검증 로직은 그냥 서비스 계층에서 호출할까? 엔티티에 캡슐화하는게 실수를 줄일 것 같긴한데. 깔끔하고.
		if (issue.getHierarchy() == EPIC || issue.getHierarchy() == SUBTASK || issue.getHierarchy() == MICROTASK) {
			throw new RuntimeException("Cannot set story point for this hierarchy level: " + issue.getHierarchy());
		}
		issue.storyPoint = storyPoint;

		return issue;
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}

	public void updateReporter(@NonNull WorkspaceMember reporter) {
		this.reporter = reporter;
	}

	public void updateTitle(@NonNull String title) {
		this.title = title;
	}

	public void updateContent(@Nullable String content) {
		this.content = content;
	}

	public void updateSummary(@Nullable String summary) {
		this.summary = summary;
	}

	public void updateDueAt(@Nullable Instant dueAt) {
		this.dueAt = dueAt;
	}

	public boolean hasParent() {
		return parentIssue != null;
	}

	public void updatePriority(IssuePriority priority) {
		this.priority = priority;
	}

	public IssueHierarchy getHierarchy() {
		return getIssueType().getIssueHierarchy();
	}

	public void updateStoryPoint(@Nullable Integer storyPoint) {
		if (storyPoint != null) {
			ensureCanUseStoryPoint();
		}
		this.storyPoint = storyPoint;
	}

	public void ensureCanUseStoryPoint() {
		// TODO: storyPoint를 사용할 수 있는 enum 값의 목록을 만들고 활용하는게 좋을까?
		//  그렇게하면 클라에서 storyPoint 사용여부를 확인하는 API를 만드는것도 편하지 않을까?
		if (getHierarchy() == EPIC || getHierarchy() == SUBTASK || getHierarchy() == MICROTASK) {
			throw new RuntimeException("Cannot set story point for this hierarchy level: " + getHierarchy());
		}
	}

	public void moveToStep(WorkflowStatus step) {
		this.currentStatus = step;
	}

	public void assignParentIssue(@NonNull Issue newParent) {
		ensureCanAddParent(newParent);
		// TODO: removeParentIssue를 여기에 캡슐화하는게 좋은 방법일까? 아니면 명시적으로 서비스에서 호출할까?
		removeParentIssue();

		this.parentIssue = newParent;
		newParent.childIssues.add(this);
	}

	// TODO: IssueHierarchy.SUBTASK, IssueHierarchy.MICROTASK는 무조건 부모가 있어야 함
	//  stand-alone 불가!
	public void removeParentIssue() {
		ensureCanRemoveParent();
		if (parentIssue != null) {
			parentIssue.getChildIssues().remove(this);
			parentIssue = null;
		}
	}

	private void ensureCanAddParent(Issue parentIssue) {
		// TODO: 어차피 서비스 계층에서 조회할때 workspace + issueKey로 조회하기 때문에 같은 워크스페이스 보장
		//  그래서 같은 워크스페이스 소속 검증 로직은 제거해도 되지 않을까?
		boolean isDifferentWorkspace = !this.getWorkspaceKey().equals(parentIssue.getWorkspaceKey());
		if (isDifferentWorkspace) {
			throw new InvalidOperationException("Parent must belong to the same workspace.");
		}

		if (this.equals(parentIssue)) {
			throw new InvalidOperationException("An issue cannot be its own parent.");
		}

		IssueHierarchy parentHierarchy = parentIssue.getIssueType().getIssueHierarchy();
		IssueHierarchy childHierarchy = this.issueType.getIssueHierarchy();

		if (parentHierarchy.isOneLevelHigher(childHierarchy)) {
			throw new InvalidOperationException(
				"Parent must be exactly one level above the child. Parent: %s (%s), Child: %s (%s)"
					.formatted(parentIssue.getIssueType().getLabel(), parentHierarchy,
						this.issueType.getLabel(), childHierarchy));
		}
	}

	public void ensureCanRemoveParent() {
		if (getHierarchy() == SUBTASK || getHierarchy() == MICROTASK) {
			throw new RuntimeException("Issues at SUBTASK or MICROTASK level must have a parent. Cannot stand alone.");
		}
	}

	// TODO: updateStartedAt: Workflow 전이에서 initial에서 다름 상태로 넘어가는 순간 호출
	// TODO: updateResolvedAt: Workflow 전이에서 terminal에 도달하는 경우 호출

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
		// TODO: 어차피 서비스 계층에서 조회할때 workspace + issueKey로 조회하기 때문에 같은 워크스페이스 보장
		//  그래서 같은 워크스페이스 소속 검증 로직은 제거해도 되지 않을까?
		// validateBelongsToWorkspace(workspaceMember);

		IssueAssignee assignee = new IssueAssignee(this, workspaceMember);

		assignees.add(assignee);
		return assignee;
	}

	public void removeAssignee(WorkspaceMember assignee) {
		IssueAssignee issueAssignee = findIssueAssignee(assignee);
		assignees.remove(issueAssignee);
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

	public boolean isAssignee(Long memberId) {
		return assignees.stream()
			.anyMatch(issueAssignee -> Objects.equals(issueAssignee.getAssigneeMemberId(), memberId));
	}

	// TODO: isReviewer()
	// TODO: isWatcher()

	// TODO: MAX_ASSIGNEES를 외부 설정값으로 설정할 수 있도록, policy 객체를 만들어서 여기에 주입해서 사용할까?
	//  아니면 검증을 서비스 계층에서하고, 해당 서비스 계층에서 policy 객체를 사용한다거나?
	private void validateAssigneeLimit() {
		if (assignees.size() >= MAX_ASSIGNEES) {
			throw new InvalidOperationException(
				String.format("The maximum number of assignees for a single issue is %d", MAX_ASSIGNEES));
		}
	}

	public Set<Long> getReviewerMemberIds() {
		Set<Long> reviewerIds = new HashSet<>();

		reviewers.stream()
			.map(IssueReviewer::getReviewerMemberId)
			.forEach(reviewerIds::add);

		return reviewerIds;
	}

	public void addReviewer(WorkspaceMember workspaceMember) {
		validateReviewerLimit();

		boolean isReviewer = reviewers.stream()
			.anyMatch(r -> r.getReviewer().getId().equals(workspaceMember.getId()));

		if (isReviewer) {
			return;
		}

		IssueReviewer reviewer = new IssueReviewer(workspaceMember, this);
		reviewers.add(reviewer);
	}

	public void removeReviewer(WorkspaceMember workspaceMember) {
		IssueReviewer issueReviewer = findIssueReviewer(workspaceMember);
		reviewers.remove(issueReviewer);
	}

	// TODO: MAX_REVIEWERS를 외부 설정값으로 설정할 수 있도록, policy 객체를 만들어서 여기에 주입해서 사용할까?
	//  아니면 검증을 서비스 계층에서하고, 해당 서비스 계층에서 policy 객체를 사용한다거나?
	private void validateReviewerLimit() {
		if (reviewers.size() >= MAX_REVIEWERS) {
			throw new InvalidOperationException(
				String.format("The max number of reviewers for a single issue is %d.", MAX_REVIEWERS));
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
