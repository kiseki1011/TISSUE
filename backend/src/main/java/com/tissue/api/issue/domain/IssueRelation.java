package com.tissue.api.issue.domain;

import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.enums.IssueType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueRelation extends WorkspaceContextBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "SOURCE_ISSUE_ID", nullable = false)
	private Issue sourceIssue;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "TARGET_ISSUE_ID", nullable = false)
	private Issue targetIssue;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueRelationType relationType;

	@Builder
	private IssueRelation(
		Issue sourceIssue,
		Issue targetIssue,
		IssueRelationType relationType
	) {
		validateSelfReference(sourceIssue, targetIssue);
		validateRelationExists(sourceIssue, targetIssue);

		this.sourceIssue = sourceIssue;
		this.targetIssue = targetIssue;
		this.relationType = relationType != null ? relationType : IssueRelationType.RELEVANT;

		validateTypeCompatibility(this.relationType);
	}

	public static void createRelation(
		Issue sourceIssue,
		Issue targetIssue,
		IssueRelationType type
	) {
		// 정방향 관계 생성
		IssueRelation relation = new IssueRelation(sourceIssue, targetIssue, type);
		sourceIssue.getOutgoingRelations().add(relation);
		targetIssue.getIncomingRelations().add(relation);

		// 역방향 관계 생성
		IssueRelation oppositeRelation = new IssueRelation(targetIssue, sourceIssue, type.getOpposite());
		targetIssue.getOutgoingRelations().add(oppositeRelation);
		sourceIssue.getIncomingRelations().add(oppositeRelation);
	}

	public static void removeRelation(Issue sourceIssue, Issue targetIssue) {
		sourceIssue.getOutgoingRelations()
			.removeIf(relation -> relation.getTargetIssue().equals(targetIssue));

		targetIssue.getOutgoingRelations()
			.removeIf(relation -> relation.getTargetIssue().equals(sourceIssue));
	}

	private void validateSelfReference(Issue sourceIssue, Issue targetIssue) {
		if (sourceIssue.equals(targetIssue)) {
			throw new InvalidOperationException("Self reference is not allowed.");
		}
	}

	private void validateRelationExists(Issue sourceIssue, Issue targetIssue) {
		boolean hasRelation = sourceIssue.getOutgoingRelations().stream()
			.anyMatch(relation -> relation.getTargetIssue().equals(targetIssue));

		if (hasRelation) {
			throw new InvalidOperationException(String.format(
				"Relation already exists. sourceIssueKey: %s, targetIssueKey: %s",
				sourceIssue.getIssueKey(), targetIssue.getIssueKey()));
		}
	}

	private void validateTypeCompatibility(IssueRelationType relationType) {
		if (relationType != IssueRelationType.DUPLICATES) {
			return;
		}

		boolean sourceIsEpic = sourceIssue.getType() == IssueType.EPIC;
		boolean targetIsEpic = targetIssue.getType() == IssueType.EPIC;

		if (sourceIsEpic != targetIsEpic) {
			throw new InvalidOperationException(
				String.format("EPIC issues can only be duplicates of other EPIC issues. Source: %s, Target: %s",
					sourceIssue.getType(), targetIssue.getType())
			);
		}

		boolean sourceIsSubTask = sourceIssue.getType() == IssueType.SUB_TASK;
		boolean targetIsSubTask = targetIssue.getType() == IssueType.SUB_TASK;

		if (sourceIsSubTask != targetIsSubTask) {
			throw new InvalidOperationException(
				String.format("SUB_TASK issues can only be duplicates of other SUB_TASK issues. Source: %s, Target: %s",
					sourceIssue.getType(), targetIssue.getType())
			);
		}
	}

	public String getWorkspaceCode() {
		return sourceIssue.getWorkspaceCode();
	}
}
