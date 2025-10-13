package com.tissue.api.issue.domain.model;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
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
		columnNames = {"source_issue_id", "target_issue_id"}
	)
)
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueRelation extends BaseEntity {

	@ToString.Include
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// TODO: toString에 포함?
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_issue_id", nullable = false)
	private Issue sourceIssue;

	// TODO: toString에 포함?
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
		ensureNotDuplicate(sourceIssue, targetIssue);
		// validateRelationType(type, sourceIssue, targetIssue);

		IssueRelation issueRelation = new IssueRelation();
		issueRelation.sourceIssue = sourceIssue;
		issueRelation.targetIssue = targetIssue;
		issueRelation.relationType = type;

		// TODO: 역방향의 관계 추가는 필요 없나?
		// 관계 형성
		sourceIssue.getOutgoingRelations().add(issueRelation);
		targetIssue.getIncomingRelations().add(issueRelation);

		return issueRelation;
	}

	void remove() {
		sourceIssue.getOutgoingRelations().remove(this);
		targetIssue.getIncomingRelations().remove(this);
	}

	public boolean isInwardFor(@NonNull Issue issue) {
		return this.targetIssue.equals(issue);
	}

	public boolean isOutwardFor(@NonNull Issue issue) {
		return this.sourceIssue.equals(issue);
	}

	/**
	 * 특정 이슈 관점에서 연결된 상대 이슈 반환
	 */
	public Issue getOtherIssue(@NonNull Issue issue) {
		if (sourceIssue.equals(issue)) {
			return targetIssue;
		}
		if (targetIssue.equals(issue)) {
			return sourceIssue;
		}
		throw new IllegalArgumentException("Issue not part of this relation");
	}

	/**
	 * 특정 이슈 관점에서의 관계 타입 (역방향이면 opposite 반환)
	 */
	public IssueRelationType getTypeFor(@NonNull Issue issue) {
		if (sourceIssue.equals(issue)) {
			return relationType; // outward
		}
		if (targetIssue.equals(issue)) {
			return relationType.getOpposite(); // inward
		}
		throw new IllegalArgumentException("Issue not part of this relation");
	}

	private static void ensureNotSelfReference(Issue sourceIssue, Issue targetIssue) {
		if (sourceIssue.equals(targetIssue)) {
			throw new InvalidOperationException("Self reference is not allowed.");
		}
	}

	private static void ensureNotDuplicate(Issue source, Issue target) {
		boolean exists = source.getOutgoingRelations().stream()
			.anyMatch(relation -> relation.getTargetIssue().equals(target));

		if (exists) {
			throw new InvalidOperationException(
				"Relation already exists. sourceIssueKey: %s, targetIssueKey: %s"
					.formatted(source.getKey(), target.getKey())
			);
		}
	}

	// TODO: 어차피 workspace + issueKey로 조회하기 때문에 무조건 같은 워크스페이스 보장됨
	//  (메서드 계약도 무조건 그렇게 하도록 노출되어 있음). 굳이 필요할까?
	private static void ensureSameWorkspace(Issue source, Issue target) {
		if (!source.getWorkspace().equals(target.getWorkspace())) {
			throw new InvalidOperationException("Issues must be in the same workspace");
		}
	}

	// TODO(optional): validateRelationType()
	//  - relation 종류별로 필요한 검증 로직을 switch 문으로
	//  - 예를 들어서 DUPLICATE 관계는 서로 같은 이슈 타입이어야 한다거나(예시 임)
}
