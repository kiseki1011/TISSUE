package com.tissue.api.issue.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.IssueRelationType;

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
		// TODO: 현재는 relation이 같은 소스-타겟 사이에서 하나만 존재 가능
		//  relation 타입까지 포함한 유니크 제약을 걸어서, 중복되지 않는 타입이라면 여러개 생성할 수 있도록 허용할까?
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
		// validateRelationType(type, sourceIssue, targetIssue);

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
			throw new InvalidOperationException("Self reference is not allowed.");
		}
	}

	private static void ensureSameWorkspace(Issue source, Issue target) {
		if (!source.getWorkspace().equals(target.getWorkspace())) {
			throw new InvalidOperationException("Issues must be in the same workspace");
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

	// TODO(optional): validateRelationType()
	//  - relation 종류별로 필요한 검증 로직을 switch 문으로
	//  - 예를 들어서 DUPLICATE 관계는 서로 같은 이슈 타입이어야 한다거나(예시)
	private static void validateRelationType(
		IssueRelationType type,
		Issue sourceIssue,
		Issue targetIssue
	) {
		switch (type) {
			case DUPLICATES, DUPLICATED_BY -> {
				// 중복은 같은 IssueType만
				if (!sourceIssue.getIssueType().equals(targetIssue.getIssueType())) {
					throw new InvalidOperationException(
						"DUPLICATES relation requires same issue type. " +
							"Source: " + sourceIssue.getIssueType().getLabel() + ", " +
							"Target: " + targetIssue.getIssueType().getLabel()
					);
				}
			}

			case BLOCKS, BLOCKED_BY -> {
				// BLOCKS는 같은 hierarchy 레벨 또는 상위 레벨만
				IssueHierarchy sourceHierarchy = sourceIssue.getHierarchy();
				IssueHierarchy targetHierarchy = targetIssue.getHierarchy();

				// Epic은 Epic만, Story는 Story/Epic, Subtask는 모두 가능
				if (sourceHierarchy == IssueHierarchy.SUBTASK &&
					targetHierarchy == IssueHierarchy.EPIC) {
					throw new InvalidOperationException(
						"Subtask cannot block Epic directly"
					);
				}
			}

			case RELEVANT -> {
				// 제약 없음
			}
		}
	}
}
