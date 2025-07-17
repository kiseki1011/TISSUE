package com.tissue.api.issue.domain.newmodel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
// @EqualsAndHashCode(of = {"event", "workflow"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTransition {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowDefinition workflow;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep fromStep;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep toStep;

	@Column(nullable = false)
	private String event; // SSM 전이 트리거 이벤트, ex: "START_PROGRESS", "MARK_DONE"

	@Column(nullable = false)
	private String label; // 사용자용 UI 라벨

	// private String guardKey;   // ex: "REQUIRES_APPROVAL", "NOT_BLOCKED"
}
