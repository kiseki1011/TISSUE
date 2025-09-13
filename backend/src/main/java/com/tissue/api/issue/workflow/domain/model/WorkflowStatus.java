package com.tissue.api.issue.workflow.domain.model;

import com.tissue.api.global.key.KeyGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
	@UniqueConstraint(columnNames = {"workflow_id", "label"})
})
@EqualsAndHashCode(of = {"workflow", "label"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// TODO: Should I use uni or bi directional relation with WorkflowDefinition?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	@Column(name = "wf_status_key", nullable = false)
	private String key;

	@Column(nullable = false)
	private String label;

	@Column(nullable = false)
	private boolean isInitial;

	@Column(nullable = false)
	private boolean isFinal;

	private String description;

	// TODO: consider adding fields for color, icons, etc...

	@PostPersist
	private void assignKey() {
		if (key == null && id != null) {
			key = KeyGenerator.generateStatusKey(id);
		}
	}

	@Builder
	public WorkflowStatus(
		Workflow workflow,
		String key,
		String label,
		Boolean isInitial,
		Boolean isFinal,
		String description
	) {
		this.workflow = workflow;
		this.key = key;
		this.label = label;
		this.isInitial = isInitial != null ? isInitial : false;
		this.isFinal = isFinal != null ? isFinal : false;
		this.description = description;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

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

	public void updateDescription(String description) {
		this.description = description;
	}

	public boolean isInitialStatus() {
		return isInitial;
	}

	public boolean isFinalStatus() {
		return isFinal;
	}
}
