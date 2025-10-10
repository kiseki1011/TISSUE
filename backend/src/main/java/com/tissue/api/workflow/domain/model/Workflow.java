package com.tissue.api.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.issue.domain.model.vo.Label;
import com.tissue.api.workflow.domain.gaurd.GuardType;
import com.tissue.api.workspace.domain.model.Workspace;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
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
public class Workflow extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@Version
	@ToString.Include
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.PERSIST, orphanRemoval = false)
	private List<WorkflowStatus> statuses = new ArrayList<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.PERSIST, orphanRemoval = false)
	private List<WorkflowTransition> transitions = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus initialStatus;

	@Column(nullable = false)
	private boolean systemProvided;

	public static Workflow create(
		@NonNull Workspace workspace,
		@NonNull Label label,
		@Nullable String description,
		@NonNull ColorType color
	) {
		Workflow wf = new Workflow();
		wf.workspace = workspace;
		wf.label = label;
		wf.description = nullToEmpty(description);
		wf.color = color;
		wf.systemProvided = false;

		return wf;
	}

	public WorkflowStatus addStatus(
		@NonNull Label label,
		@Nullable String description,
		@NonNull ColorType color,
		boolean initial,
		boolean terminal
	) {
		ensureNotSystemProvided();
		ensureUniqueStatusLabel(label);

		WorkflowStatus status = WorkflowStatus.of(label, description, color, initial, terminal);
		attachStatus(status);

		return status;
	}

	public WorkflowTransition addTransition(
		@NonNull Label label,
		@Nullable String description,
		@NonNull WorkflowStatus source,
		@NonNull WorkflowStatus target
	) {
		ensureNotSystemProvided();
		ensureUniqueTransitionLabelForSource(label, source);
		ensureNoDuplicateEdge(source, target);

		WorkflowTransition transition = WorkflowTransition.of(label, description, source, target);
		attachTransition(transition);

		return transition;
	}

	public void setAsSystemProvided() {
		this.systemProvided = true;
	}

	public void rename(@NonNull Label label) {
		ensureNotSystemProvided();
		this.label = label;
	}

	public void updateDescription(@Nullable String desc) {
		this.description = nullToEmpty(desc);
	}

	public void updateColor(@Nullable ColorType color) {
		this.color = color;
	}

	public void updateInitialStatus(@NonNull WorkflowStatus newInitial) {
		ensureNotSystemProvided();
		for (WorkflowStatus s : statuses) {
			s.unmarkInitial();
		}
		newInitial.markInitial();
		this.initialStatus = newInitial;
	}

	public List<WorkflowStatus> getFinalStatuses() {
		return statuses.stream()
			.filter(WorkflowStatus::isTerminal)
			.toList();
	}

	// TODO: 삭제 금지 정책을 정하자
	//  전략 1: 하나 이상의 Issue가 intial status가 아니면서 Workflow를 진행 중이면 삭제 막기
	//  전략 2: 하나 이상의 IssueType이 해당 Workflow를 선택했으면 삭제 막기
	public void softDelete() {
		ensureNotSystemProvided();
		archive();
		statuses.forEach(WorkflowStatus::softDelete);
		transitions.forEach(WorkflowTransition::softDelete);
	}

	public void softDeleteStatus(WorkflowStatus status) {
		ensureNotSystemProvided();
		status.softDelete();
		statuses.remove(status);
	}

	public void softDeleteTransition(WorkflowTransition transition) {
		ensureNotSystemProvided();
		transition.softDelete();
		transitions.remove(transition);
	}

	public void renameStatus(@NonNull WorkflowStatus status, @NonNull Label newLabel) {
		ensureNotSystemProvided();
		if (status.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueStatusLabel(newLabel);
		status.updateLabel(newLabel);
	}

	public void renameTransition(@NonNull WorkflowTransition transition, @NonNull Label newLabel) {
		ensureNotSystemProvided();
		if (transition.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueTransitionLabelForSource(newLabel, transition.getSourceStatus());
		transition.updateLabel(newLabel);
	}

	public void updateStatusTerminalFlag(@NonNull WorkflowStatus status, boolean terminalFlag) {
		ensureNotSystemProvided();
		if (status.isTerminal() == terminalFlag) {
			return;
		}
		if (terminalFlag) {
			status.markTerminal();
			return;
		}
		status.unmarkTerminal();
	}

	public void rewireTransitionSource(@NonNull WorkflowTransition transition, @NonNull WorkflowStatus newSource) {
		ensureNotSystemProvided();
		transition.rewireSource(newSource);
	}

	public void rewireTransitionTarget(@NonNull WorkflowTransition transition, @NonNull WorkflowStatus newTarget) {
		ensureNotSystemProvided();
		transition.rewireTarget(newTarget);
	}

	public void addTransitionGuard(@NonNull WorkflowTransition transition, @NonNull GuardType guardType,
		@Nullable String params, int order) {
		transition.addGuard(guardType, params, order);
	}

	public void clearGuardsForTransition(@NonNull WorkflowTransition transition) {
		transition.clearGuards();
	}

	private void attachStatus(WorkflowStatus status) {
		status.attachToWorkflow(this);
		statuses.add(status);

		if (status.isInitial()) {
			updateInitialStatus(status);
		}
	}

	private void attachTransition(WorkflowTransition transition) {
		transition.attachToWorkflow(this);
		transitions.add(transition);
	}

	private void ensureNotSystemProvided() {
		if (systemProvided) {
			throw new RuntimeException("Cannot modify system provided workflow.");
		}
	}

	// TODO: 이게 메서드는 문제 없겠지?
	private void ensureNoDuplicateEdge(WorkflowStatus source, WorkflowStatus target) {
		boolean dup = transitions.stream()
			.anyMatch(x -> x.getSourceStatus().equals(source) && x.getTargetStatus().equals(target));
		if (dup) {
			throw new DuplicateResourceException("Duplicate transition (source,target) is not allowed.");
		}
	}

	private void ensureUniqueStatusLabel(Label newLabel) {
		boolean dup = statuses.stream()
			.anyMatch(s -> s.getLabel().equals(newLabel));
		if (dup) {
			throw new DuplicateResourceException("Duplicate status label: " + newLabel);
		}
	}

	private void ensureUniqueTransitionLabelForSource(Label newLabel, WorkflowStatus source) {
		boolean dup = transitions.stream()
			.filter(t -> t.getSourceStatus().equals(source))
			.anyMatch(t -> t.getLabel().equals(newLabel));
		if (dup) {
			throw new DuplicateResourceException(
				"Duplicate transition label for source '" + source.getLabel() + "': " + newLabel);
		}
	}
}
