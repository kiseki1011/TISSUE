package com.tissue.api.issue.domain.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueRelation;

@Component
public class DfsRelationCycleDetector implements RelationCycleDetector {

	@Override
	public void ensureNoCycle(
		Issue source,
		Issue target,
		IssueRelationType relationType
	) {
		if (hasCycle(source, target, relationType)) {
			throw new InvalidOperationException(
				"Creating this relation would create a cycle. %s %s %s forms a circular dependency."
					.formatted(source.getKey(), relationType, target.getKey())
			);
		}
	}

	/**
	 * target에서 출발해서 source에 도달 가능한지 확인
	 */
	private boolean hasCycle(
		Issue source,
		Issue target,
		IssueRelationType relationType
	) {
		Set<Issue> visited = new HashSet<>();
		return dfs(target, source, relationType, visited);
	}

	/**
	 * DFS로 그래프 순회
	 *
	 * @param current 현재 노드
	 * @param destination 목적지 (source)
	 * @param relationType 따라갈 관계 타입
	 * @param visited 방문한 노드들
	 * @return destination에 도달하면 true
	 */
	private boolean dfs(
		Issue current,
		Issue destination,
		IssueRelationType relationType,
		Set<Issue> visited
	) {
		// 이미 방문했으면 스킵
		if (!visited.add(current)) {
			return false;
		}

		// 목적지 도달 → 사이클 발견
		if (current.equals(destination)) {
			return true;
		}

		// 현재 노드의 outgoing 관계 중 같은 타입만 따라감
		for (IssueRelation relation : current.getOutgoingRelations()) {
			// 같은 타입의 관계만
			if (relation.getRelationType() != relationType) {
				continue;
			}

			Issue nextIssue = relation.getTargetIssue();

			if (dfs(nextIssue, destination, relationType, visited)) {
				return true; // 사이클 발견
			}
		}

		return false;
	}
}
