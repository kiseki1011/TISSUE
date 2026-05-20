package com.tissue.feature.issue.domain.service.relation;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueRelation;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.exception.RelationCycleDetectedException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

// TODO: see annotation
@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        evaluation = Evaluation.PERFORMANCE_PROBLEM,
        evaluationReason = """
      - has a N+1 problem
      - using recursive has a potential stack-overflow possibility
      - maybe 3~4 relations might not be a problem, but will see problems if relations become deeper
      - possible solutions are 1) recursive CTE, 2) bulk loading before search
    """,
        reviewedBy = "kiseki1011",
        model = "claude-opus-4-5")
@Component
public class DfsRelationCycleDetector implements RelationCycleDetector {

    @Override
    public void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType) {
        if (!relationType.requiresAcyclicCheck()) {
            return;
        }

        List<String> cyclePath = new ArrayList<>();
        if (findPath(target, source, new HashSet<>(), cyclePath)) {
            cyclePath.addFirst(source.getKey());
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
                pathTrace.addFirst(current.getKey());
                return true;
            }
        }

        return false;
    }
}
