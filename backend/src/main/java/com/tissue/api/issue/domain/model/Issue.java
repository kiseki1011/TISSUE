package com.tissue.api.issue.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;
import static com.tissue.api.common.util.TextNormalizer.*;
import static com.tissue.api.issue.domain.enums.IssueRelationType.*;

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
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.issue.domain.service.IssueStoryPointsAggregator;
import com.tissue.api.issue.domain.service.ProgressType;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.sprint.domain.model.SprintIssue;
import com.tissue.api.workflow.domain.model.WorkflowState;
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
	private String content;

	@Lob
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssuePriority priority;

	private Instant startedAt;

	private Instant resolvedAt;

	private Instant dueAt;

	private Integer storyPoint;

	private Integer countBasedProgress;

	private Integer pointBasedProgress;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_issue_id")
	private Issue parentIssue;

	@OneToMany(mappedBy = "parentIssue")
	private List<Issue> childIssues = new ArrayList<>();

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
	private WorkflowState currentState;

	public static Issue create(
		@NonNull Workspace workspace,
		@NonNull IssueType issueType,
		@NonNull WorkspaceMember reporter,
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
		issue.reporter = reporter;
		issue.title = title;
		issue.content = nullToEmpty(content);
		issue.summary = nullToEmpty(summary);
		issue.priority = priority == null ? IssuePriority.NORMAL : priority;
		issue.dueAt = requireFutureOrPresent(dueAt);

		ensureCanUseStoryPoint(issue.getHierarchy(), storyPoint);
		issue.storyPoint = storyPoint;

		return issue;
	}

	public void changeReporter(@NonNull WorkspaceMember reporter) {
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
		this.dueAt = requireFutureOrPresent(dueAt);
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

	// EPIC 전용
	public void updateTotalStoryPoints() {
		this.storyPoint = IssueStoryPointsAggregator.calculateTotalStoryPoints(this);
	}

	public void updateProgress(@Nullable Integer countBased, @Nullable Integer pointBased) {
		this.countBasedProgress = countBased;
		this.pointBasedProgress = pointBased;
	}

	public IssueRelation addRelation(Issue targetIssue, IssueRelationType type) {
		return IssueRelation.create(this, targetIssue, type);
	}

	// TODO: removeRelation 내부의 로직을 IssueRelation에서 담당하는 형태로 만드는건 불가능할까?
	//  Issue에서는 단순히 해당 IssueRelation의 메서드를 부르고. 불가능한가?
	public void removeRelation(Issue otherIssue) {
		IssueRelation outgoing = outgoingRelations.stream()
			.filter(r -> r.getTargetIssue().equals(otherIssue))
			.findFirst()
			.orElse(null);

		if (outgoing != null) {
			outgoing.remove();
			return;
		}

		IssueRelation incoming = incomingRelations.stream()
			.filter(r -> r.getSourceIssue().equals(otherIssue))
			.findFirst()
			.orElse(null);

		if (incoming != null) {
			incoming.remove();
			return;
		}

		throw new ResourceNotFoundException(
			"No relation found between %s and %s".formatted(this.getKey(), otherIssue.getKey())
		);

	}

	public void proceedToNextState(@NonNull WorkflowState newState) {
		WorkflowState previousState = this.currentState;
		this.currentState = newState;

		if (previousState.isInitial() && this.startedAt == null) {
			this.startedAt = Instant.now();
		}
		if (newState.isTerminal() && this.resolvedAt == null) {
			this.resolvedAt = Instant.now();
		}
		if (previousState.isTerminal() && !newState.isTerminal()) {
			this.resolvedAt = null;
		}
	}

	// TODO: reporter, assignee, reviewers를 설정 및 추가할때 subcribers에 자동으로 추가 되도록 설계해야 할까?
	public void addSubscriber(@NonNull WorkspaceMember workspaceMember) {
		IssueSubscriber subscriber = new IssueSubscriber(workspaceMember);
		subscribers.add(subscriber);
	}

	public void removeSubscriber(@NonNull WorkspaceMember workspaceMember) {
		subscribers.removeIf(issueSubscriber -> issueSubscriber.getSubscriber().equals(workspaceMember));
	}

	public void assignTo(@NonNull WorkspaceMember assignee) {
		this.assignee = assignee;
	}

	public void unassign() {
		this.assignee = null;
	}

	public void addReviewer(@NonNull WorkspaceMember workspaceMember) {
		boolean isReviewer = reviewers.stream()
			.anyMatch(r -> r.getReviewer().equals(workspaceMember));

		if (isReviewer) {
			return;
		}

		IssueReviewer reviewer = new IssueReviewer(workspaceMember, this);
		reviewers.add(reviewer);
	}

	public void removeReviewer(@NonNull WorkspaceMember workspaceMember) {
		reviewers.removeIf(issueReviewer -> issueReviewer.getReviewer().equals(workspaceMember));
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

	// TODO: 삭제에 필요한 검증 로직을 issueValidator.ensureDeletable()로 분리
	public void softDelete() {
		if (!currentState.isInitial()) {
			throw new RuntimeException("Cannot delete issue that is not initial state.");
		}

		if (!childIssues.isEmpty()) {
			throw new RuntimeException("Cannot delete issue that has children.");
		}

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

	public boolean isReporter(@NonNull WorkspaceMember wm) {
		return Objects.equals(reporter, wm);
	}

	public boolean isAssignee(@NonNull WorkspaceMember wm) {
		return Objects.equals(assignee, wm);
	}

	public boolean isSubscriber(@NonNull WorkspaceMember wm) {
		return subscribers.stream()
			.anyMatch(s -> Objects.equals(s.getSubscriber(), wm));
	}

	public boolean isReviewer(@NonNull WorkspaceMember wm) {
		return reviewers.stream()
			.anyMatch(s -> Objects.equals(s.getReviewer(), wm));
	}

	public boolean isParticipant(@NonNull WorkspaceMember wm) {
		return isAuthor(wm.getMemberId()) ||
			isReporter(wm) ||
			isAssignee(wm) ||
			isReviewer(wm) ||
			isSubscriber(wm);
	}

	public List<IssueRelation> getAllRelations() {
		List<IssueRelation> all = new ArrayList<>();
		all.addAll(outgoingRelations);
		all.addAll(incomingRelations);
		return all;
	}

	public Integer getProgress(ProgressType type) {
		return switch (type) {
			case COUNT_BASED -> countBasedProgress;
			case POINT_BASED -> pointBasedProgress;
		};
	}

	public List<Issue> getRelatedIssuesByType(IssueRelationType type) {
		List<Issue> result = new ArrayList<>();

		outgoingRelations.stream()
			.filter(r -> r.getRelationType() == type)
			.map(IssueRelation::getTargetIssue)
			.forEach(result::add);

		incomingRelations.stream()
			.filter(r -> r.getRelationType() == type.getOpposite())
			.map(IssueRelation::getSourceIssue)
			.forEach(result::add);

		return result;
	}

	public boolean isBlockedBy(Issue otherIssue) {
		return incomingRelations.stream()
			.anyMatch(r -> r.getSourceIssue().equals(otherIssue) && r.getRelationType() == BLOCKS);
	}

	public List<Issue> getBlockingIssues() {
		return getRelatedIssuesByType(BLOCKS);
	}

	public List<Issue> getBlockedByIssues() {
		return getRelatedIssuesByType(BLOCKED_BY);
	}

	private static void ensureCanUseStoryPoint(IssueHierarchy hierarchy, Integer storyPoint) {
		if (storyPoint == null) {
			return;
		}
		if (hierarchy.cannotHaveStoryPoint()) {
			throw new InvalidOperationException("Cannot set story point for hierarchy: " + hierarchy);
		}
	}

	private void detachFromCurrentParent() {
		if (parentIssue != null) {
			parentIssue.getChildIssues().remove(this);
			parentIssue = null;
		}
	}

	// TODO: 어디까지가 불변식이고, 어디까지가 정책인가?
	private void ensureCanSetParent(@NonNull Issue parentIssue) {
		boolean isDifferentWorkspace = !this.getWorkspace().equals(parentIssue.getWorkspace());
		if (isDifferentWorkspace) {
			throw new InvalidOperationException("Parent must belong to the same workspace.");
		}

		if (this.equals(parentIssue)) {
			throw new InvalidOperationException("An issue cannot be its own parent.");
		}

		// TODO: cannotHaveParent()가 boolean을 반환하는게 아니라 아예 검증을 수행해서 예외를 던지도록 설계할까?
		if (getHierarchy().cannotHaveParent()) {
			throw new RuntimeException("EPIC level issues cannot have parents.");
		}

		IssueHierarchy parentHierarchy = parentIssue.getHierarchy();
		IssueHierarchy childHierarchy = this.getHierarchy();

		// TODO: canBeParentOf()가 boolean을 반환하는게 아니라 아예 검증을 수행해서 예외를 던지도록 설계할까?
		if (!parentHierarchy.canBeParentOf(childHierarchy)) {
			throw new InvalidOperationException(
				"Parent must be exactly one level above the child. Parent: %s (%s), Child: %s (%s)"
					.formatted(parentIssue.getIssueType().getLabel(), parentHierarchy,
						this.issueType.getLabel(), childHierarchy));
		}
	}

	private void ensureCanRemoveParent() {
		// TODO: mustHaveParent()가 boolean을 반환하는게 아니라 아예 검증을 수행해서 예외를 던지도록 설계할까?
		if (getHierarchy().mustHaveParent()) {
			throw new RuntimeException("Issues at SUBTASK or MICROTASK level must have a parent. Cannot stand alone.");
		}
	}

	private void clearParticipants() {
		unassign();
		this.reviewers.clear();
		this.subscribers.clear();
	}

	private void clearRelations() {
		this.outgoingRelations.clear();
		this.incomingRelations.clear();
	}
}
