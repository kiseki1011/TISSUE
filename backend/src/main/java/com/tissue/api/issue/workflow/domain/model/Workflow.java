package com.tissue.api.issue.workflow.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
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

	@Column(nullable = false)
	@ToString.Include
	private String label;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkflowStatus> statuses = new HashSet<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkflowTransition> transitions = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus initialStatus;

	@Column(nullable = false)
	private String description;

	@Builder
	public Workflow(
		Workspace workspace,
		String label,
		String description
	) {
		this.workspace = workspace;
		this.label = label;
		this.description = description;
	}

	public static Workflow create(
		@NonNull Workspace workspace,
		@NonNull String label,
		@Nullable String description
	) {
		Workflow wf = new Workflow();
		wf.workspace = workspace;
		wf.label = label;
		wf.description = description;

		return wf;
	}

	public void addStatus(@NonNull WorkflowStatus status) {
		statuses.add(status);
		status.setWorkflow(this);

		if (status.isInitialStatus()) {
			updateInitialStatus(status);
		}
	}

	public void addTransition(@NonNull WorkflowTransition transition) {
		transitions.add(transition);
		transition.setWorkflow(this);
	}

	public void updateLabel(@NonNull String label) {
		this.label = label;
	}

	public void updateInitialStatus(@NonNull WorkflowStatus newInitialStatus) {
		if (!statuses.contains(newInitialStatus)) {
			throw new InvalidOperationException("The step must be part of this workflow.");
		}

		for (WorkflowStatus status : statuses) {
			status.setInitial(false);
		}

		newInitialStatus.setInitial(true);
		this.initialStatus = newInitialStatus;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}

	public List<WorkflowStatus> getFinalStatuses() {
		return statuses.stream()
			.filter(WorkflowStatus::isFinal)
			.toList();
	}
}
