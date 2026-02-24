package com.tissue.feature.issue.domain.service.relation;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueRelationType;

public interface RelationCycleDetector {

    /**
     * Ensures that adding a relation does not create a cycle in the relation graph.
     * @param source The starting issue of the relation
     * @param target The destination issue of the relation
     * @param relationType The type of the relation (ex: BLOCKS, CAUSES)
     */
    void ensureNoCycle(Issue source, Issue target, IssueRelationType relationType);
}
