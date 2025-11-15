package com.tissue.api.issue.domain.service.relation;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.exception.IssueRelationCircularDependencyException;

@Component
public class DfsRelationCycleDetector implements RelationCycleDetector {

	@Override
	public void ensureNoCycle(
		Issue source,
		Issue target,
		IssueRelationType relationType
	) {
		if (hasCycle(source, target)) {
			throw new IssueRelationCircularDependencyException(relationType, source.getKey(), target.getKey());
		}
	}

	private boolean hasCycle(
		Issue source,
		Issue target
	) {
		Set<Issue> visited = new HashSet<>();
		return dfs(target, source, visited);
	}

	/**
	 * DFS로 그래프 순회
	 *
	 * @param current 현재 노드
	 * @param destination 목적지 (source)
	 * @param visited 방문한 노드들
	 * @return destination에 도달하면 true
	 */
	private boolean dfs(
		Issue current,
		Issue destination,
		Set<Issue> visited
	) {
		if (!visited.add(current)) {
			return false;
		}
		if (current.equals(destination)) {
			return true;
		}

		for (IssueRelation relation : current.getRelations().getOutgoingRelations()) {
			if (!relation.getRelationType().requiresAcyclicCheck()) {
				continue;
			}

			Issue nextIssue = relation.getTargetIssue();
			if (dfs(nextIssue, destination, visited)) {
				return true;
			}
		}
		return false;
	}
}
