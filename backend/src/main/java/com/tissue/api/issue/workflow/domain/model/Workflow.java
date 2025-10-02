package com.tissue.api.issue.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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

	@OneToMany(mappedBy = "workflow", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
	private List<WorkflowStatus> statuses = new ArrayList<>();

	@OneToMany(mappedBy = "workflow", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
	private List<WorkflowTransition> transitions = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus initialStatus;

	public static Workflow create(
		@NonNull Workspace workspace,
		@NonNull Label label,
		@Nullable String description
	) {
		Workflow wf = new Workflow();
		wf.workspace = workspace;
		wf.label = label;
		wf.description = nullToEmpty(description);

		return wf;
	}

	public WorkflowStatus addStatus(
		@NonNull Label label,
		@Nullable String description,
		boolean initial,
		boolean terminal
	) {
		ensureUniqueStatusLabel(label);

		WorkflowStatus status = WorkflowStatus.of(label, description, initial, terminal);
		attachStatus(status);

		return status;
	}

	public WorkflowTransition addTransition(
		@NonNull Label label,
		@Nullable String description,
		@NonNull WorkflowStatus source,
		@NonNull WorkflowStatus target
	) {
		ensureNoDuplicateEdge(source, target);
		ensureTransitionAllowed(source, target);
		ensureUniqueTransitionLabelForSource(label, source);

		WorkflowTransition transition = WorkflowTransition.of(label, description, source, target, false);
		attachTransition(transition);

		return transition;
	}

	public void rename(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String desc) {
		this.description = nullToEmpty(desc);
	}

	public void updateInitialStatus(@NonNull WorkflowStatus newInitial) {
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

	// TODO: 하나 이상의 Issue가 Workflow를 진행 중이면 서비스 계층에서 막기
	//  - ensureDeletable 같은 메서드를 구현해서 사용
	//  - Issue가 initial status 또는 terminal status에 있다면 ok
	public void softDelete() {
		archive();
		statuses.forEach(WorkflowStatus::softDelete);
		transitions.forEach(WorkflowTransition::softDelete);
	}

	public void defineMainFlow(@NonNull List<WorkflowTransition> transitionPath) {
		for (var t : transitions) {
			t.excludeFromMainFlow();
		}
		for (var t : transitionPath) {
			t.includeInMainFlow();
		}
	}

	public void renameStatus(@NonNull WorkflowStatus status, @NonNull Label newLabel) {
		if (status.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueStatusLabel(newLabel);
		status.updateLabel(newLabel);
	}

	public void renameTransition(@NonNull WorkflowTransition transition, @NonNull Label newLabel) {
		if (transition.getLabel().equals(newLabel)) {
			return;
		}
		ensureUniqueTransitionLabelForSource(newLabel, transition.getSourceStatus());
		transition.updateLabel(newLabel);
	}

	public void markStatusTerminal(@NonNull WorkflowStatus status) {
		status.markTerminal();
	}

	public void unmarkStatusTerminal(@NonNull WorkflowStatus status) {
		status.unmarkTerminal();
	}

	public void rewireTransitionSource(@NonNull WorkflowTransition transition, @NonNull WorkflowStatus newSource) {
		transition.rewireSource(newSource);
	}

	public void rewireTransitionTarget(@NonNull WorkflowTransition transition, @NonNull WorkflowStatus newTarget) {
		transition.rewireTarget(newTarget);
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
	
	private void ensureTransitionAllowed(WorkflowStatus source, WorkflowStatus target) {
		if (target.isInitial()) {
			throw new InvalidOperationException("Transitions cannot enter the initial status.");
		}
		if (source.equals(target)) {
			throw new InvalidOperationException("Self-loop not allowed.");
		}
	}

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
