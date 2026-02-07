package com.tissue.issue.domain.service.relation;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueRelation;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issue.domain.exception.RelationCycleDetectedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

// TODO: Add javadoc
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

            throw new RelationCycleDetectedException(source.getKey(), target.getKey(), relationType, cyclePath);
        }
    }

    private boolean findPath(Issue current, Issue destination, Set<Issue> visited, List<String> pathTrace) {
        if (!visited.add(current)) {
            return false;
        }

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
