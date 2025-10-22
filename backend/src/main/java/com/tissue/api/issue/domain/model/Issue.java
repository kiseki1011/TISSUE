package com.tissue.api.issue.domain.model;

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

	// TODO: title도 IssueContent VO에 포함시킬까?
	@Column(nullable = false)
	private String title;

	@Embedded
	private IssueContent content;

	@Embedded
	private IssueSchedule schedule;

	@Embedded
	private IssueProgress progress;

	@Embedded
	private IssueParticipants participants;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssuePriority priority;

	// TODO: 이것도 VO로 만들까 그냥?
	private Integer storyPoint;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_issue_id")
	private Issue parentIssue;

	@OneToMany(mappedBy = "parentIssue")
	private List<Issue> childIssues = new ArrayList<>();

	// TODO: relation 관련도 IssueRelations이라는 VO로 만들까?
	@OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> outgoingRelations = new HashSet<>();

	@OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> incomingRelations = new HashSet<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<SprintIssue> sprintIssues = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private IssueType issueType;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowState currentState;

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
		issue.priority = priority == null ? IssuePriority.NORMAL : priority;

		ensureCanUseStoryPoint(issue.getHierarchy(), storyPoint);
		issue.storyPoint = storyPoint;

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

	// EPIC 전용
	public void updateTotalStoryPoints() {
		this.storyPoint = this.getChildIssues().stream()
			.filter(child -> child.getStoryPoint() != null)
			.mapToInt(Issue::getStoryPoint)
			.sum();
	}

	public void updateProgress(@Nullable Integer countBased, @Nullable Integer pointBased) {
		this.progress.update(countBased, pointBased);
	}

	public IssueRelation addRelation(Issue targetIssue, IssueRelationType type) {
		return IssueRelation.create(this, targetIssue, type);
	}

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
		this.participants.addSubscriber(workspaceMember);
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

	// TODO: 삭제에 필요한 검증 로직을 issueValidator.ensureDeletable()로 분리할까?
	private void ensureDeletable() {
		if (!currentState.isInitial()) {
			throw new RuntimeException("Cannot delete issue that is not initial state.");
		}
		// TODO: 자식 이슈가 있어도 삭제를 허용할까? (자식 이슈가 SUBTASK 이하라면 삭제 안됨)
		if (!childIssues.isEmpty()) {
			throw new RuntimeException("Cannot delete issue that has children.");
		}
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

	/** ---------------TODO: IssueRelations VO 만들고 해당 VO로 분리?-------------- **/
	public List<IssueRelation> getAllRelations() {
		List<IssueRelation> all = new ArrayList<>();
		all.addAll(outgoingRelations);
		all.addAll(incomingRelations);
		return all;
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

	/** --------------------------------------------------------------------------- **/

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
		participants.clear();
	}

	private void clearRelations() {
		this.outgoingRelations.clear();
		this.incomingRelations.clear();
	}

	@Override
	public String toString() {
		return "Issue{" +
			"id=" + id +
			", key='" + key + '\'' +
			", workspaceKey=" + getWorkspaceKey() +
			", title='" + title + '\'' +
			'}';
	}
}
