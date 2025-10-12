package com.tissue.api.issue.domain.model;

import static com.tissue.api.common.util.TextNormalizer.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;
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
import lombok.ToString;

@Entity
@SQLRestriction("archived = false")
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends BaseEntity {

	// TODO: use application.yml for value
	private static final int MAX_REVIEWERS = 10;
	private static final int MAX_ASSIGNEES = 50;

	@ToString.Include
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ToString.Include
	@Column(name = "issue_key", nullable = false)
	private String key;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace_id", nullable = false)
	private Workspace workspace;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private WorkspaceMember reporter;

	@ToString.Include
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

	// TODO: 이슈의 상태를 전이(transition) 시킬 때 intial에서 다음 상태로 가는 경우 설정(변경 불가해야 함)
	private Instant startedAt;

	// TODO: 이슈의 상태를 전이 시킬 때 terminal에 도달하는 경우 설정
	private Instant resolvedAt;

	@Column(nullable = false)
	private Instant dueAt;

	private Integer storyPoint;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_issue_id")
	private Issue parentIssue;

	// TODO: Set vs List? 둘 중 뭐가 더 좋으려나?
	@OneToMany(mappedBy = "parentIssue")
	private Set<Issue> childIssues = new HashSet<>();

	@OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> outgoingRelations = new HashSet<>();

	@OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> incomingRelations = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private WorkspaceMember assignee;

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueReviewer> reviewers = new HashSet<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueSubscriber> subscribers = new HashSet<>();

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
		issue.content = nullToEmpty(content);
		issue.summary = nullToEmpty(summary);
		issue.priority = priority;
		issue.dueAt = dueAt;

		ensureCanUseStoryPoint(issue.getHierarchy(), storyPoint);
		issue.storyPoint = storyPoint;

		return issue;
	}

	// TODO: updateReporter면 충분히 좋은 이름인가? 아니면 더 좋은 이름이 있을까?
	public void updateReporter(@NonNull WorkspaceMember reporter) {
		this.reporter = reporter;
	}

	public void updateTitle(@NonNull String title) {
		this.title = title;
	}

	public void updateContent(@Nullable String content) {
		this.content = nullToEmpty(content);
	}

	public void updateSummary(@Nullable String summary) {
		this.summary = nullToEmpty(summary);
	}

	public void updateDueAt(@Nullable Instant dueAt) {
		this.dueAt = dueAt;
	}

	public void updatePriority(IssuePriority priority) {
		this.priority = priority;
	}

	public void updateStoryPoint(@Nullable Integer storyPoint) {
		if (storyPoint != null) {
			ensureCanUseStoryPoint(this.getHierarchy(), storyPoint);
		}
		this.storyPoint = storyPoint;
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}

	public IssueHierarchy getHierarchy() {
		return issueType.getIssueHierarchy();
	}

	private static void ensureCanUseStoryPoint(IssueHierarchy hierarchy, Integer storyPoint) {
		if (storyPoint == null) {
			return;
		}
		if (hierarchy.cannotHaveStoryPoint()) {
			throw new InvalidOperationException(
				"Cannot set story point for hierarchy: " + hierarchy
			);
		}
	}

	public void moveToStep(WorkflowStatus step) {
		this.currentStatus = step;
	}

	public void assignParentIssue(@NonNull Issue newParent) {
		ensureCanAddParent(newParent);
		removeParentIssue();

		this.parentIssue = newParent;
		newParent.childIssues.add(this);
	}

	public void removeParentIssue() {
		ensureCanRemoveParent();
		if (parentIssue != null) {
			parentIssue.getChildIssues().remove(this);
			parentIssue = null;
		}
	}

	public void ensureCanAddParent(Issue parentIssue) {
		// TODO: 어차피 서비스 계층에서 조회할때 workspace + issueKey로 조회하기 때문에 같은 워크스페이스 보장함.
		//  그래서 같은 워크스페이스 소속 검증 로직은 제거해도 되지 않을까?
		//  애초에 노출되는 조회 메서드 자체가 workspace + issueKey로 찾도록 강제함
		boolean isDifferentWorkspace = !this.getWorkspace().equals(parentIssue.getWorkspace());
		if (isDifferentWorkspace) {
			throw new InvalidOperationException("Parent must belong to the same workspace.");
		}

		if (this.equals(parentIssue)) {
			throw new InvalidOperationException("An issue cannot be its own parent.");
		}

		if (getHierarchy().cannotHaveParent()) {
			throw new RuntimeException("EPIC level issues cannot have parents.");
		}

		IssueHierarchy parentHierarchy = parentIssue.getHierarchy();
		IssueHierarchy childHierarchy = this.getHierarchy();

		if (parentHierarchy.isOneLevelHigher(childHierarchy)) {
			throw new InvalidOperationException(
				"Parent must be exactly one level above the child. Parent: %s (%s), Child: %s (%s)"
					.formatted(parentIssue.getIssueType().getLabel(), parentHierarchy,
						this.issueType.getLabel(), childHierarchy));
		}
	}

	public void ensureCanRemoveParent() {
		if (getHierarchy().mustHaveParent()) {
			throw new RuntimeException("Issues at SUBTASK or MICROTASK level must have a parent. Cannot stand alone.");
		}
	}

	// TODO: updateStartedAt: Workflow 전이에서 initial에서 다름 상태로 넘어가는 순간 호출
	// TODO: updateResolvedAt: Workflow 전이에서 terminal에 도달하는 경우 호출

	public boolean isAuthor(@NonNull Long memberId) {
		return Objects.equals(getCreatedBy(), memberId);
	}

	// TODO: 이슈 삭제 전략을 어떻게 가져가야 할까? (일단 기본적으로 soft-delete)
	//  - 현재 워크플로우 진행중인 이슈는 삭제 불가?
	//  - 특정 이슈의 부모라면 삭제 불가? 아니면 자동으로 자식 이슈까지 다같이 삭제?
	//  - 확실한건 IssueHierarchy.SUBTASK, MICROTASK의 부모라면 삭제 제한해야 함
	//  - 설정된 IssueRelation과 관련해서 삭제 정책을 어떻게 가져갈지도 정해야 함
	public void softDelete() {
		archive();
	}

	public void addSubscriber(@NonNull WorkspaceMember workspaceMember) {
		IssueSubscriber watcher = new IssueSubscriber(workspaceMember);
		subscribers.add(watcher);
	}

	public void removeSubscriber(@NonNull WorkspaceMember workspaceMember) {
		subscribers.removeIf(watcher -> watcher.getSubscriber().equals(workspaceMember));
	}

	// TODO: setAssignee 또는 updateAssignee 대신 assignTo가 좋으려나?
	public void assignTo(@NonNull WorkspaceMember assignee) {
		// TODO: 어차피 서비스 계층에서 조회할때 workspace + issueKey로 조회하기 때문에 같은 워크스페이스 보장함.
		//  그래서 같은 워크스페이스 소속 검증 로직은 제거해도 되지 않을까?
		//  애초에 노출되는 조회 메서드 자체가 workspace + issueKey로 찾도록 강제함
		// validateBelongsToWorkspace(assignee);
		this.assignee = assignee;
	}

	public void unassign() {
		this.assignee = null;
	}

	// TODO: WorkspaceMember를 입력 파라미터로 받을까?
	public boolean isAssignee(@NonNull Long memberId) {
		return Objects.equals(assignee.getMemberId(), memberId);
	}

	// TODO: isReviewer()
	// TODO: isSubscriber()

	// TODO: calculateEpicLevelStoryPoint()

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
}
