package com.tissue.api.issue.domain.newmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

// TODO: Have I set the UniqueConstraint properly?
//  A WorkflowStep must be unique for each WorkflowDefinition by label.
@Entity
@Getter
@Table(uniqueConstraints = {
	@UniqueConstraint(columnNames = {"workflow_id", "label"})
})
@EqualsAndHashCode(of = {"workflow", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowStep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private WorkflowDefinition workflow;

	@Column(nullable = false)
	private String key; // used for SSM

	@Column(nullable = false)
	private String label; // UI label

	@Column(nullable = false)
	private boolean isInitial;

	@Column(nullable = false)
	private boolean isFinal;

	// TODO: consider adding fields for color, icons, etc...

	@Builder
	public WorkflowStep(
		WorkflowDefinition workflow,
		String key,
		String label,
		Boolean isInitial,
		Boolean isFinal
	) {
		this.workflow = workflow;
		this.key = key;
		this.label = label;
		this.isInitial = isInitial != null ? isInitial : false;
		this.isFinal = isFinal != null ? isFinal : false;
	}

	public void setWorkflow(WorkflowDefinition workflow) {
		this.workflow = workflow;
	}

	// TODO: Dont i need to make the original initial step's "isInitial" value to false?
	public void setInitial(boolean isInitial) {
		this.isInitial = isInitial;
	}

	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}

	public void updateLabel(String label) {
		this.label = label;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isInitialStep() {
		return isInitial;
	}

	public boolean isFinalStep() {
		return isFinal;
	}
}
