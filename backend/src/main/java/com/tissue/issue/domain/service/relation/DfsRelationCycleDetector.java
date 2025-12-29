package com.tissue.issue.domain.service.relation;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issue.domain.exception.IssueExceptions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DfsRelationCycleDetector implements RelationCycleDetector {

    @Override
    public void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType) {
        if (!relationType.requiresAcyclicCheck()) {
            return;
        }

        List<String> cyclePath = new ArrayList<>();
        if (findPath(target, source, new HashSet<>(), cyclePath)) {
            cyclePath.add(0, source.getKey());
            cyclePath.add(source.getKey());

            throw IssueExceptions.relationCycleDetected(source.getKey(), target.getKey(), relationType, cyclePath);
        }
    }

    /**
     * DFS로 경로를 탐색하며 기록하는 메서드
     *
     * @param current 현재 노드
     * @param destination 목표 노드 (Source)
     * @param visited 방문 체크
     * @param pathTrace 경로를 기록할 리스트 (성공 시에만 채워짐)
     * @return 목적지 도달 시 true
     */
    private boolean findPath(Issue current, Issue destination, Set<Issue> visited, List<String> pathTrace) {
        // 이미 방문한 노드면 사이클 방지를 위해 중단
        if (!visited.add(current)) {
            return false;
        }

        // 목적지 도달
        if (current.equals(destination)) {
            pathTrace.add(current.getKey());
            return true;
        }

        for (IssueRelation relation : current.getRelations().getOutgoingRelations()) {
            if (!relation.getRelationType().requiresAcyclicCheck()) {
                continue;
            }

            Issue nextIssue = relation.getTargetIssue();

            if (findPath(nextIssue, destination, visited, pathTrace)) {
                pathTrace.add(0, current.getKey());
                return true;
            }
        }

        return false;
    }
}
