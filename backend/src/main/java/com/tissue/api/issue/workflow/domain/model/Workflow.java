package com.tissue.api.issue.workflow.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.global.key.KeyGenerator;
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
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Am I setting the @UniqueConstraint right?
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workspace_id", "label"})
})
@EqualsAndHashCode(of = {"workspace", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workflow extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@Column(name = "workflow_key", nullable = false)
	private String key;

	@Column(nullable = false)
	private String label;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkflowStatus> statuses = new HashSet<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<WorkflowTransition> transitions = new HashSet<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus initialStatus;

	private String description;

	@PostPersist
	private void assignKey() {
		if (key == null && id != null) {
			key = KeyGenerator.generateWorkflowKey(id);
		}
	}

	@Builder
	public Workflow(
		Workspace workspace,
		String key,
		String label,
		String description
	) {
		this.workspace = workspace;
		this.key = key;
		this.label = label;
		this.description = description;
	}

	public void addStatus(WorkflowStatus status) {
		statuses.add(status);
		status.setWorkflow(this);

		if (status.isInitialStatus()) {
			updateInitialStatus(status);
		}
	}

	public void addTransition(WorkflowTransition transition) {
		transitions.add(transition);
		transition.setWorkflow(this);
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void updateInitialStatus(WorkflowStatus newInitialStatus) {
		if (!statuses.contains(newInitialStatus)) {
			throw new InvalidOperationException("The step must be part of this workflow.");
		}

		for (WorkflowStatus status : statuses) {
			status.setInitial(false);
		}

		newInitialStatus.setInitial(true);
		this.initialStatus = newInitialStatus;
	}

	public void updateDescription(String description) {
		this.description = description;
	}

	public List<WorkflowStatus> getFinalStatuses() {
		return statuses.stream()
			.filter(WorkflowStatus::isFinal)
			.toList();
	}
}
