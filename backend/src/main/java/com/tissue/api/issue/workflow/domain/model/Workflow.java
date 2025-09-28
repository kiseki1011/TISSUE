package com.tissue.api.issue.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.DuplicateResourceException;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workflow extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkflowStatus> statuses = new ArrayList<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
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
		WorkflowStatus status = WorkflowStatus.of(label, description, initial, terminal);
		attachStatus(status);

		return status;
	}

	public WorkflowTransition addTransition(
		@NonNull Label label,
		@Nullable String description,
		@NonNull WorkflowStatus source,
		@NonNull WorkflowStatus target,
		boolean mainFlow
	) {
		// ensureOwned(source);
		// ensureOwned(target);
		ensureNoDuplicateEdge(source, target);

		WorkflowTransition transition = WorkflowTransition.of(label, description, source, target, mainFlow);
		attachTransition(transition);

		return transition;
	}

	public void updateLabel(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	public void updateInitialStatus(@NonNull WorkflowStatus status) {
		// ensureOwned(status);

		for (WorkflowStatus s : statuses) {
			s._unmarkInitial();
		}

		status._markInitial();
		this.initialStatus = status;
	}

	public List<WorkflowStatus> getFinalStatuses() {
		return statuses.stream()
			.filter(WorkflowStatus::isTerminal)
			.toList();
	}

	// TODO: 하나 이상의 IssueType이 Workflow를 사용 중이면 서비스 계층에서 막기
	//  - ensureDeletable 같은 메서드를 구현해서 사용
	public void softDelete() {
		archive();
		statuses.forEach(WorkflowStatus::softDelete);
		transitions.forEach(WorkflowTransition::softDelete);
	}

	// TODO: 호출전에 검증을 위해 ensureMainFlowSingleLine 호출 필요
	public void defineMainFlow(@NonNull List<WorkflowTransition> transitionPath) {
		for (var t : transitions) {
			t._excludeFromMainFlow();
		}
		for (var t : transitionPath) {
			t._includeInMainFlow();
		}
	}

	public void renameStatus(WorkflowStatus status, Label newLabel) {
		// ensureOwned(status);
		// TODO: Workflow 스코프 내에서 WorkflowStatus의 Label이 유일하도록 검증(서비스 계층에서 해도 괜찮을 듯)
		// ensureUniqueLabel(newLabel);
		status._updateLabel(newLabel);
	}

	public void renameTransition(WorkflowTransition transition, Label newLabel) {
		// ensureOwned(transition);
		// TODO: Workflow 스코프 내에서 WorkflowTransition의 Label이 유일하도록 검증(서비스 계층에서 해도 괜찮을 듯)
		//  - 아니면 transition 정도는 Label 중복을 허용할까?
		// ensureUniqueLabel(newLabel);
		transition._updateLabel(newLabel);
	}

	private void attachStatus(WorkflowStatus status) {
		status._attachToWorkflow(this);
		statuses.add(status);

		if (status.isInitial()) {
			updateInitialStatus(status);
		}
	}

	private void attachTransition(WorkflowTransition transition) {
		transition._attachToWorkflow(this);
		transitions.add(transition);
	}

	// private void ensureOwned(WorkflowStatus status) {
	// 	if (status.getWorkflow() != this) {
	// 		throw new InvalidOperationException("Status not owned by this workflow.");
	// 	}
	// }
	//
	// private void ensureOwned(WorkflowTransition transition) {
	// 	if (transition.getWorkflow() != this) {
	// 		throw new InvalidOperationException("Transition not owned by this workflow.");
	// 	}
	// }

	private void ensureNoDuplicateEdge(WorkflowStatus source, WorkflowStatus target) {
		boolean dup = transitions.stream()
			.anyMatch(x -> x.getSourceStatus() == source && x.getTargetStatus() == target);
		if (dup) {
			throw new DuplicateResourceException("Duplicate transition(source,target) is not allowed.");
		}
	}
}
