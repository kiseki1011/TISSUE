package com.tissue.api.issue.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

// TODO: archived=true 대상만으로 유니크 제약을 위한 Postgres DDL 적용
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

	public void addStatus(@NonNull WorkflowStatus status) {
		statuses.add(status);
		status.setWorkflow(this);

		if (status.isInitial()) {
			updateInitialStatus(status);
		}
	}

	public void addTransition(@NonNull WorkflowTransition transition) {
		transitions.add(transition);
		transition.setWorkflow(this);
	}

	public void updateLabel(@NonNull Label label) {
		this.label = label;
	}

	public void updateInitialStatus(@NonNull WorkflowStatus newInitialStatus) {
		if (!statuses.contains(newInitialStatus)) {
			throw new InvalidOperationException("The step must be part of this workflow.");
		}

		for (WorkflowStatus status : statuses) {
			status.markInitialFlag(false);
		}

		newInitialStatus.markInitialFlag(true);
		this.initialStatus = newInitialStatus;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}

	public List<WorkflowStatus> getFinalStatuses() {
		return statuses.stream()
			.filter(WorkflowStatus::isTerminal)
			.toList();
	}

	public void softDelete() {
		archive();
		// TODO: statuses, transitions에 대해서도 softDelete 전파
	}
}
