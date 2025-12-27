package com.tissue.workflow.domain;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.workflow.domain.enums.StateCategory;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@SQLRestriction("softDeleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowState extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	@Embedded
	private Name name;

	@Column(nullable = false, length = 255)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ColorType color;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StateCategory category;

	static WorkflowState of(
		@NonNull Name name,
		@Nullable String description,
		@NonNull ColorType color,
		@NonNull StateCategory category
	) {
		WorkflowState ws = new WorkflowState();
		ws.name = name;
		ws.description = description;
		ws.color = color;
		ws.category = category;

		return ws;
	}

	void attachToWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	void updateName(@NonNull Name name) {
		this.name = name;
	}

	public void updateDescription(@Nullable String description) {
		this.description = description;
	}

	public void updateColor(@NonNull ColorType color) {
		this.color = color;
	}

	void categorizeAs(@NonNull StateCategory category) {
		this.category = category;
	}

	public String getDisplayName() {
		return name.getDisplay();
	}

	public boolean isCategorizedAs(StateCategory category) {
		return getCategory() == category;
	}
}
