package com.tissue.api.workflow.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.model.vo.Label;
import com.tissue.api.workflow.domain.gaurd.GuardType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
public class WorkflowTransition extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	@Version
	@ToString.Include
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	private Workflow workflow;

	@Embedded
	@ToString.Include
	private Label label;

	@Column(nullable = false, length = 255)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus sourceStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStatus targetStatus;

	@OneToMany(mappedBy = "transition", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("executionOrder ASC")
	private List<TransitionGuardConfig> guardConfigs = new ArrayList<>();

	public static WorkflowTransition of(
		@NonNull Label label,
		@Nullable String description,
		@NonNull WorkflowStatus sourceStatus,
		@NonNull WorkflowStatus targetStatus
	) {
		WorkflowTransition wt = new WorkflowTransition();
		wt.label = label;
		wt.description = nullToEmpty(description);
		wt.sourceStatus = sourceStatus;
		wt.targetStatus = targetStatus;

		return wt;
	}

	void updateLabel(@NonNull Label label) {
		this.label = label;
	}

	public void updateDescription(@Nullable String description) {
		this.description = nullToEmpty(description);
	}

	void attachToWorkflow(@NonNull Workflow workflow) {
		this.workflow = workflow;
	}

	void rewireSource(@NonNull WorkflowStatus sourceStatus) {
		this.sourceStatus = sourceStatus;
	}

	void rewireTarget(@NonNull WorkflowStatus targetStatus) {
		this.targetStatus = targetStatus;
	}

	// Guard 추가 (GuardType enum 사용)
	void addGuard(@NonNull GuardType guardType, @Nullable String params, int order) {
		TransitionGuardConfig config = TransitionGuardConfig.create(
			this,
			guardType,
			params,
			order
		);
		guardConfigs.add(config);
	}

	// 모든 Guard 제거
	void clearGuards() {
		guardConfigs.clear();
	}

	void softDelete() {
		archive();
	}
}
