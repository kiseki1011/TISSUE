package com.tissue.api.issue.domain.newmodel;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
// @EqualsAndHashCode(of = {"name", "issueType", "workspace"}, callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowDefinition extends BaseEntity {

	// TODO: EqualsAndHashCode를 name, issueType(IssueTypeDefinition 필드), workspace 기준으로 적용?
	// TODO: UniqueConstraint를 name, issueType(IssueTypeDefinition 필드), workspace 기준으로 적용?

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	private Workspace workspace;

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkflowStep> steps = new ArrayList<>();

	@OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<WorkflowTransition> transitions = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	private WorkflowStep initialStep;

	public List<WorkflowStep> getFinalSteps() {
		return steps.stream()
			.filter(WorkflowStep::isFinal)
			.toList();
	}
}
