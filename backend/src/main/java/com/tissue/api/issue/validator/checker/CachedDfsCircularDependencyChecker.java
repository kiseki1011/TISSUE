package com.tissue.api.issue.validator.checker;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.enums.IssueRelationType;

public class CachedDfsCircularDependencyChecker implements CircularDependencyChecker {

	private final Cache<String, Set<String>> dependencyCache;

	public CachedDfsCircularDependencyChecker(int cacheSize, int cacheDuration) {
		this.dependencyCache = Caffeine.newBuilder()
			.maximumSize(cacheSize)
			.expireAfterWrite(cacheDuration, TimeUnit.HOURS)
			.build();
	}

	@Override
	public void validateNoCircularDependency(Issue sourceIssue, Issue targetIssue) {
		String sourceKey = sourceIssue.getWorkspaceCode() + ":" + sourceIssue.getIssueKey();

		Set<String> cached = dependencyCache.getIfPresent(sourceKey);
		if (cached != null && cached.contains(targetIssue.getIssueKey())) {
			throw new InvalidOperationException("Circular dependency detected in blocking chain.");
		}

		Set<String> reachableIssues = new HashSet<>();
		Set<Issue> visited = new HashSet<>();
		visited.add(sourceIssue);

		try {
			collectReachableIssues(targetIssue, visited, reachableIssues);
			dependencyCache.put(sourceKey, reachableIssues);
		} catch (InvalidOperationException e) {
			dependencyCache.put(sourceKey, reachableIssues);
			throw e;
		}
	}

	private void collectReachableIssues(Issue current, Set<Issue> visited, Set<String> reachableIssues) {
		reachableIssues.add(current.getIssueKey());

		for (IssueRelation relation : current.getOutgoingRelations()) {
			if (relation.getRelationType() != IssueRelationType.BLOCKS) {
				continue;
			}

			Issue nextIssue = relation.getTargetIssue();
			if (!visited.add(nextIssue)) {
				throw new InvalidOperationException("Circular dependency detected in blocking chain.");
			}

			collectReachableIssues(nextIssue, visited, reachableIssues);
			visited.remove(nextIssue);
		}
	}
}
