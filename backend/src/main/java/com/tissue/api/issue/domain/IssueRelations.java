package com.tissue.api.issue.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssueRelationType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueRelations {

	@OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> outgoingRelations = new HashSet<>();

	@OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueRelation> incomingRelations = new HashSet<>();

	public static IssueRelations init() {
		return new IssueRelations();
	}

	public IssueRelation addRelation(Issue sourceIssue, Issue targetIssue, IssueRelationType type) {
		ensureNoRelationExists(sourceIssue, targetIssue);
		return IssueRelation.create(sourceIssue, targetIssue, type);
	}

	public void removeRelation(Issue otherIssue) {
		outgoingRelations.removeIf(r -> r.getTargetIssue().equals(otherIssue));
		incomingRelations.removeIf(r -> r.getSourceIssue().equals(otherIssue));
	}

	public void clear() {
		outgoingRelations.clear();
		incomingRelations.clear();
	}

	public List<IssueRelation> getAll() {
		List<IssueRelation> all = new ArrayList<>();
		all.addAll(outgoingRelations);
		all.addAll(incomingRelations);
		return all;
	}

	// 이 이슈가 blocking하는 이슈들
	public List<Issue> getBlockingIssues() {
		return outgoingRelations.stream()
			.filter(r -> r.getRelationType() == IssueRelationType.BLOCKS)
			.map(IssueRelation::getTargetIssue)
			.toList();
	}

	// 이 이슈를 blocking하는 이슈들
	public List<Issue> getBlockedByIssues() {
		return incomingRelations.stream()
			.filter(r -> r.getRelationType() == IssueRelationType.BLOCKS)
			.map(IssueRelation::getSourceIssue)
			.toList();
	}

	public List<Issue> getRelevantIssues() {
		List<Issue> result = new ArrayList<>();

		outgoingRelations.stream()
			.filter(r -> r.getRelationType() == IssueRelationType.RELEVANT)
			.map(IssueRelation::getTargetIssue)
			.forEach(result::add);

		incomingRelations.stream()
			.filter(r -> r.getRelationType() == IssueRelationType.RELEVANT)
			.map(IssueRelation::getSourceIssue)
			.forEach(result::add);

		return result;
	}

	// 이 이슈가 복제한 이슈들
	public List<Issue> getDuplicates() {
		return outgoingRelations.stream()
			.filter(r -> r.getRelationType() == IssueRelationType.DUPLICATES)
			.map(IssueRelation::getTargetIssue)
			.toList();
	}

	// 이 이슈를 복제한 이슈들
	public List<Issue> getDuplicatedBy() {
		return incomingRelations.stream()
			.filter(r -> r.getRelationType() == IssueRelationType.DUPLICATES)
			.map(IssueRelation::getSourceIssue)
			.toList();
	}

	public int getTotalRelationCount() {
		return outgoingRelations.size() + incomingRelations.size();
	}

	public boolean hasRelationWith(Issue otherIssue) {
		return outgoingRelations.stream()
			.anyMatch(r -> r.getTargetIssue().equals(otherIssue)) ||
			incomingRelations.stream()
				.anyMatch(r -> r.getSourceIssue().equals(otherIssue));
	}

	public boolean isBlockedBy(Issue otherIssue) {
		return incomingRelations.stream()
			.anyMatch(r -> r.getSourceIssue().equals(otherIssue) &&
				r.getRelationType() == IssueRelationType.BLOCKS);
	}

	public boolean isDuplicateOf(Issue otherIssue) {
		return incomingRelations.stream()
			.anyMatch(r -> r.getSourceIssue().equals(otherIssue) &&
				r.getRelationType() == IssueRelationType.DUPLICATES);
	}

	private static void ensureNoRelationExists(Issue source, Issue target) {
		boolean exists = source.getRelations().getOutgoingRelations().stream()
			.anyMatch(relation -> relation.getTargetIssue().equals(target));

		if (exists) {
			throw new InvalidOperationException(
				"Relation already exists. sourceIssueKey: %s, targetIssueKey: %s"
					.formatted(source.getKey(), target.getKey())
			);
		}
	}
}
