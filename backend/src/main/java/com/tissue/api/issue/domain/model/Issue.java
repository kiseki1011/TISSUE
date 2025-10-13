package com.tissue.api.issue.domain.model;

import static com.tissue.api.common.util.TextNormalizer.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueRelationType;
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

	private static int MAX_REVIEWERS = 10;

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

	private Instant dueAt; // TODO: 현재 시간 보다 이전으로 설정 못하도록 검증 필요

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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private WorkspaceMember assignee;

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueReviewer> reviewers = new HashSet<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueSubscriber> subscribers = new HashSet<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<SprintIssue> sprintIssues = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private IssueType issueType;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus currentStatus;

	// TODO: IssueConfig에서 @PostConstruct를 사용하는 것 보다 좋은 방법은 없나?
	public static void setLimits(int maxReviewers) {
		MAX_REVIEWERS = maxReviewers;
	}

	public static Issue create(
		@NonNull Workspace workspace,
		@NonNull IssueType issueType,
		@NonNull String title,
		@Nullable String content,
		@Nullable String summary,
		@Nullable IssuePriority priority,
		@Nullable Instant dueAt,
		@Nullable Integer storyPoint
	) {
		Issue issue = new Issue();
		issue.workspace = workspace;
		issue.issueType = issueType;
		issue.title = title;
		issue.content = nullToEmpty(content);
		issue.summary = nullToEmpty(summary);
		issue.priority = priority == null ? IssuePriority.NORMAL : priority;
		issue.dueAt = dueAt;

		ensureCanUseStoryPoint(issue.getHierarchy(), storyPoint);
		issue.storyPoint = storyPoint;

		return issue;
	}

	public void setReporter(@NonNull WorkspaceMember reporter) {
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

	public void updatePriority(@NonNull IssuePriority priority) {
		this.priority = priority;
	}

	public void updateStoryPoint(@Nullable Integer storyPoint) {
		if (storyPoint != null) {
			ensureCanUseStoryPoint(this.getHierarchy(), storyPoint);
		}
		this.storyPoint = storyPoint;
	}

	public IssueRelation addRelation(Issue targetIssue, IssueRelationType type) {
		return IssueRelation.create(this, targetIssue, type);
	}

	public void removeRelation(Issue otherIssue) {
		// Outgoing 찾기
		IssueRelation outgoing = outgoingRelations.stream()
			.filter(r -> r.getTargetIssue().equals(otherIssue))
			.findFirst()
			.orElse(null);

		if (outgoing != null) {
			outgoing.remove();
			return;
		}

		// Incoming 찾기
		IssueRelation incoming = incomingRelations.stream()
			.filter(r -> r.getSourceIssue().equals(otherIssue))
			.findFirst()
			.orElse(null);

		if (incoming != null) {
			incoming.remove();
			return;
		}

		throw new RuntimeException("No relation found between " + this.getKey() + " and " + otherIssue.getKey());

	}

	/**
	 * 모든 관계 조회 (양방향)
	 */
	public List<IssueRelation> getAllRelations() {
		List<IssueRelation> all = new ArrayList<>();
		all.addAll(outgoingRelations);
		all.addAll(incomingRelations);
		return all;
	}

	/**
	 * 특정 타입의 관련 이슈들 조회
	 */
	public List<Issue> getRelatedIssues(IssueRelationType type) {
		List<Issue> result = new ArrayList<>();

		// Outgoing에서 찾기
		outgoingRelations.stream()
			.filter(r -> r.getRelationType() == type)
			.map(IssueRelation::getTargetIssue)
			.forEach(result::add);

		// Incoming에서 역방향 타입으로 찾기
		incomingRelations.stream()
			.filter(r -> r.getRelationType() == type.getOpposite())
			.map(IssueRelation::getSourceIssue)
			.forEach(result::add);

		return result;
	}

	/**
	 * BLOCKS 관계 확인
	 */
	public boolean isBlockedBy(Issue otherIssue) {
		return incomingRelations.stream()
			.anyMatch(r ->
				r.getSourceIssue().equals(otherIssue) &&
					r.getRelationType() == IssueRelationType.BLOCKS
			);
	}

	/**
	 * Blocking하는 이슈들
	 */
	public List<Issue> getBlockingIssues() {
		return getRelatedIssues(IssueRelationType.BLOCKS);
	}

	/**
	 * Blocked by 이슈들
	 */
	public List<Issue> getBlockedByIssues() {
		return getRelatedIssues(IssueRelationType.BLOCKED_BY);
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

	public void moveToStatus(@NonNull WorkflowStatus status) {
		this.currentStatus = status;
	}

	public void assignParentIssue(@NonNull Issue newParent) {
		ensureCanAddParent(newParent);
		detachFromCurrentParent();

		this.parentIssue = newParent;
		newParent.childIssues.add(this);
	}

	public void removeParentIssue() {
		ensureCanRemoveParent();
		detachFromCurrentParent();
	}

	private void detachFromCurrentParent() {
		if (parentIssue != null) {
			parentIssue.getChildIssues().remove(this);
			parentIssue = null;
		}
	}

	private void ensureCanAddParent(@NonNull Issue parentIssue) {
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

	private void ensureCanRemoveParent() {
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
	//  - terminal 상태의 이슈는 삭제 불가?(기록으로 무조건 남도록?)
	public void softDelete() {
		unassign();
		this.reviewers.clear();
		this.subscribers.clear();

		// TODO: relations clear

		detachFromCurrentParent();

		archive();
	}

	public void addSubscriber(@NonNull WorkspaceMember workspaceMember) {
		IssueSubscriber watcher = new IssueSubscriber(workspaceMember);
		subscribers.add(watcher);
	}

	public void removeSubscriber(@NonNull WorkspaceMember workspaceMember) {
		subscribers.removeIf(watcher -> watcher.getSubscriber().equals(workspaceMember));
	}

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
	// TODO: calculateEpicProgress()
	//  전략 1: (해결된 STORY 레벨 이슈들의 story point 합) / (EPIC 레벨 이슈의 story point)
	//  전력 2: (해결된 STORY 레벨 이슈들의 수) / (전체 STORY 레벨 이슈들의 수)

	public void addReviewer(@NonNull WorkspaceMember workspaceMember) {
		ensureCanAddReviewer();

		boolean isReviewer = reviewers.stream()
			.anyMatch(r -> r.getReviewer().getId().equals(workspaceMember.getId()));

		if (isReviewer) {
			return;
		}

		IssueReviewer reviewer = new IssueReviewer(workspaceMember, this);
		reviewers.add(reviewer);
	}

	public void removeReviewer(@NonNull WorkspaceMember workspaceMember) {
		IssueReviewer issueReviewer = findIssueReviewer(workspaceMember);
		reviewers.remove(issueReviewer);
	}

	private void ensureCanAddReviewer() {
		if (reviewers.size() >= MAX_REVIEWERS) {
			throw new InvalidOperationException(String.format("The max number of reviewers is %d.", MAX_REVIEWERS));
		}
	}

	private IssueReviewer findIssueReviewer(WorkspaceMember workspaceMember) {
		return reviewers.stream()
			.filter(r -> r.getReviewer().equals(workspaceMember))
			.findFirst()
			.orElseThrow(() -> new ForbiddenOperationException(
				String.format("Not a reviewer assigned to this issue. workspaceMemberId: %d, displayName: %s",
					workspaceMember.getId(), workspaceMember.getDisplayName()))
			);
	}
}
