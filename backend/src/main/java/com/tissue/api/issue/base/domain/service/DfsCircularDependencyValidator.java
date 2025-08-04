package com.tissue.api.issue.base.domain.service;

import java.util.HashSet;
import java.util.Set;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.enums.IssueRelationType;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueRelation;
import com.tissue.api.issue.base.domain.service.cache.IssueRelationDependencyCache;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DfsCircularDependencyValidator implements CircularDependencyValidator {

	private final IssueRelationDependencyCache dependencyCache;

	@Override
	public void validateNoCircularDependency(Issue sourceIssue, Issue targetIssue) {
		// 캐시 키 생성
		String cacheKey = generateCacheKey(sourceIssue);

		// 캐시 확인
		Set<String> cachedDependencies = dependencyCache.get(cacheKey);
		if (cachedDependencies != null && cachedDependencies.contains(targetIssue.getKey())) {
			throw new InvalidOperationException("Circular dependency detected in blocking chain.");
		}

		// DFS로 순환 참조 검사 및 도달 가능한 이슈 수집
		Set<String> reachableIssues = new HashSet<>();
		Set<Issue> visited = new HashSet<>();
		visited.add(sourceIssue);

		try {
			collectReachableIssues(targetIssue, visited, reachableIssues);
			// 성공적으로 검사가 완료되면 캐시 업데이트
			dependencyCache.put(cacheKey, reachableIssues);
		} catch (InvalidOperationException e) {
			// 예외가 발생해도 지금까지 수집된 정보는 캐시에 저장
			dependencyCache.put(cacheKey, reachableIssues);
			throw e;
		}
	}

	/**
	 * Collects all reachable issues using DFS.
	 */
	private void collectReachableIssues(Issue current, Set<Issue> visited, Set<String> reachableIssues) {
		reachableIssues.add(current.getKey());

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

	/**
	 * Creates cache key.
	 */
	private String generateCacheKey(Issue issue) {
		return issue.getWorkspaceCode() + ":" + issue.getKey();
	}
}
