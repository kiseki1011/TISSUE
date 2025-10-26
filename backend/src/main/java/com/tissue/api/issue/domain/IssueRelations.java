package com.tissue.api.issue.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
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
		ensureNotDuplicate(sourceIssue, targetIssue);
		return IssueRelation.create(sourceIssue, targetIssue, type);
	}

	public void removeRelation(Issue thisIssue, Issue otherIssue) {
		boolean removed = outgoingRelations.removeIf(r -> r.getTargetIssue().equals(otherIssue));

		if (removed) {
			return;
		}

		removed = incomingRelations.removeIf(r -> r.getSourceIssue().equals(otherIssue));

		if (removed) {
			return;
		}

		// TODO: 굳이 던져야 하나? 관계가 없으면 그냥 무시해도 되지 않을까?
		throw new ResourceNotFoundException(
			"No relation found between %s and %s".formatted(thisIssue.getKey(), otherIssue.getKey())
		);
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

	public List<Issue> getRelatedIssuesByType(IssueRelationType type) {
		List<Issue> result = new ArrayList<>();

		outgoingRelations.stream()
			.filter(r -> r.getRelationType() == type)
			.map(IssueRelation::getTargetIssue)
			.forEach(result::add);

		incomingRelations.stream()
			.filter(r -> r.getRelationType() == type.getOpposite())
			.map(IssueRelation::getSourceIssue)
			.forEach(result::add);

		return result;
	}

	public boolean isBlockedBy(Issue otherIssue) {
		return incomingRelations.stream()
			.anyMatch(r -> r.getSourceIssue().equals(otherIssue) &&
				r.getRelationType() == IssueRelationType.BLOCKS);
	}

	public List<Issue> getBlockingIssues() {
		return getRelatedIssuesByType(IssueRelationType.BLOCKS);
	}

	public List<Issue> getBlockedByIssues() {
		return getRelatedIssuesByType(IssueRelationType.BLOCKED_BY);
	}

	public List<Issue> getRelevantIssues() {
		return getRelatedIssuesByType(IssueRelationType.RELEVANT);
	}

	public List<Issue> getDuplicates() {
		return getRelatedIssuesByType(IssueRelationType.DUPLICATES);
	}

	public boolean hasRelationWith(Issue otherIssue) {
		return outgoingRelations.stream()
			.anyMatch(r -> r.getTargetIssue().equals(otherIssue)) ||
			incomingRelations.stream()
				.anyMatch(r -> r.getSourceIssue().equals(otherIssue));
	}

	public int getTotalRelationCount() {
		return outgoingRelations.size() + incomingRelations.size();
	}

	private static void ensureNotDuplicate(Issue source, Issue target) {
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
