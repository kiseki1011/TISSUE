package com.tissue.issue.domain;

import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issue.domain.exception.RelationAlreadyExistsException;
import com.tissue.issue.domain.exception.RelationNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Embeddable
@Getter
public class IssueRelations {

    @OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueRelation> outgoingRelations = new HashSet<>();

    @OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<IssueRelation> incomingRelations = new HashSet<>();

    @SuppressWarnings("NullAway.Init")
    protected IssueRelations() {}

    static IssueRelations init() {
        return new IssueRelations();
    }

    IssueRelation addRelation(Issue sourceIssue, Issue targetIssue, IssueRelationType type) {
        ensureNoRelationExists(sourceIssue, targetIssue);
        return IssueRelation.create(sourceIssue, targetIssue, type);
    }

    IssueRelation removeRelation(Issue sourceIssue, Issue otherIssue) {
        Iterator<IssueRelation> iterator = outgoingRelations.iterator();
        while (iterator.hasNext()) {
            IssueRelation relation = iterator.next();

            if (relation.getTargetIssue().equals(otherIssue)) {
                iterator.remove();
                relation.getTargetIssue().getRelations().removeIncomingInternal(relation);
                return relation;
            }
        }

        throw new RelationNotFoundException(sourceIssue.getWorkspaceKey(), sourceIssue.getKey(), otherIssue.getKey());
    }

    void clear() {
        outgoingRelations.clear();
        incomingRelations.clear();
    }

    public List<IssueRelation> getAll() {
        List<IssueRelation> all = new ArrayList<>();
        all.addAll(outgoingRelations);
        all.addAll(incomingRelations);
        return all;
    }

    /**
     * @return List of issues that this issue BLOCKS
     */
    public List<Issue> getBlockingIssues() {
        return outgoingRelations.stream()
                .filter(r -> r.getRelationType() == IssueRelationType.BLOCKS)
                .map(IssueRelation::getTargetIssue)
                .toList();
    }

    /**
     * @return List of issues that BLOCKS this issue
     */
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

    /**
     * @return List of issues that this issue DUPLICATES
     */
    public List<Issue> getDuplicates() {
        return outgoingRelations.stream()
                .filter(r -> r.getRelationType() == IssueRelationType.DUPLICATES)
                .map(IssueRelation::getTargetIssue)
                .toList();
    }

    /**
     * @return List of issues that DUPLICATES this issue
     */
    public List<Issue> getDuplicatedBy() {
        return incomingRelations.stream()
                .filter(r -> r.getRelationType() == IssueRelationType.DUPLICATES)
                .map(IssueRelation::getSourceIssue)
                .toList();
    }

    private static void ensureNoRelationExists(Issue source, Issue target) {
        boolean exists = source.getRelations().getOutgoingRelations().stream()
                .anyMatch(relation -> relation.getTargetIssue().equals(target));

        if (exists) {
            throw new RelationAlreadyExistsException(source.getWorkspaceKey(), source.getKey(), target.getKey());
        }
    }

    private void removeIncomingInternal(IssueRelation relation) {
        this.incomingRelations.remove(relation);
    }
}
