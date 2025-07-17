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
// @EqualsAndHashCode(of = {"name", "workflow"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowStep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowDefinition workflow;

	@Column(nullable = false, unique = true)
	private String name; // SSM 내부용, ex: "TODO", "IN_PROGRESS"

	@Column(nullable = false)
	private String label; // 사용자용 UI 라벨, 국제화를 위해 message.properties 사용?

	private boolean isFinal;

	// TODO: 색상 지정, 아이콘 등도 필드로 추가하는 것을 고려
}
