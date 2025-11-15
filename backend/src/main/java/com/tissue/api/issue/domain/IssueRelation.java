package com.tissue.api.issue.domain;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.exception.IssueSelfReferenceException;
import com.tissue.api.issue.exception.IssueTypeMismatchForRelationException;
import com.tissue.api.issue.exception.RelationWorkspaceMismatchException;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Table(name = "issue_relation",
	uniqueConstraints = @UniqueConstraint(
		columnNames = {"source_issue_id", "target_issue_id"}
	)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueRelation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_issue_id", nullable = false)
	private Issue sourceIssue;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_issue_id", nullable = false)
	private Issue targetIssue;

	@ToString.Include
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueRelationType relationType;

	static IssueRelation create(
		@NonNull Issue sourceIssue,
		@NonNull Issue targetIssue,
		@NonNull IssueRelationType type
	) {
		ensureSameWorkspace(sourceIssue, targetIssue);
		ensureNotSelfReference(sourceIssue, targetIssue);
		validateRelationType(type, sourceIssue, targetIssue);

		IssueRelation issueRelation = new IssueRelation();
		issueRelation.sourceIssue = sourceIssue;
		issueRelation.targetIssue = targetIssue;
		issueRelation.relationType = type;

		sourceIssue.getRelations().getOutgoingRelations().add(issueRelation);
		targetIssue.getRelations().getIncomingRelations().add(issueRelation);

		return issueRelation;
	}

	private static void ensureNotSelfReference(Issue sourceIssue, Issue targetIssue) {
		if (sourceIssue.equals(targetIssue)) {
			throw new IssueSelfReferenceException(sourceIssue.getKey());
		}
	}

	private static void ensureSameWorkspace(Issue source, Issue target) {
		if (!source.getWorkspace().equals(target.getWorkspace())) {
			throw new RelationWorkspaceMismatchException(
				source.getWorkspaceKey(),
				source.getKey(),
				target.getWorkspaceKey(),
				target.getKey()
			);
		}
	}

	private static void validateRelationType(
		IssueRelationType type,
		Issue sourceIssue,
		Issue targetIssue
	) {
		switch (type) {
			case DUPLICATES -> {
				boolean issueTypeNotMatch = !sourceIssue.getIssueType().equals(targetIssue.getIssueType());
				if (issueTypeNotMatch) {
					throw new IssueTypeMismatchForRelationException(
						type,
						sourceIssue.getIssueType(),
						targetIssue.getIssueType(),
						sourceIssue.getWorkspaceKey(),
						sourceIssue.getKey(),
						targetIssue.getKey()
					);
				}
			}
			case BLOCKS, RELEVANT -> {
			}
		}
	}

	@Override
	public String toString() {
		return String.format(
			"IssueRelation(id=%d, source=%s, target=%s, type=%s)",
			id,
			sourceIssue != null ? sourceIssue.getKey() : "?",
			targetIssue != null ? targetIssue.getKey() : "?",
			relationType
		);
	}
}
