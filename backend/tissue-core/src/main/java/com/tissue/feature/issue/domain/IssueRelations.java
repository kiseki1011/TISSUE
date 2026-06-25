package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.RELATION_ALREADY_EXISTS;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.RELATION_NOT_FOUND;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
@Embeddable
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

    IssueRelation removeRelation(Issue otherIssue) {
        // Outgoing (this issue is the source): removable for any relation type.
        Iterator<IssueRelation> outgoing = outgoingRelations.iterator();
        while (outgoing.hasNext()) {
            IssueRelation relation = outgoing.next();

            if (relation.getTargetIssue().equals(otherIssue)) {
                outgoing.remove();
                relation.getTargetIssue().getRelations().removeIncomingInternal(relation);
                return relation;
            }
        }
        // Incoming (this issue is the target): only RELEVANT is symmetric, so it may be
        // removed from either side. Directional relations (BLOCKS/CAUSES/DUPLICATES) must
        // be removed from their source, so they are not removable here.
        Iterator<IssueRelation> incoming = incomingRelations.iterator();
        while (incoming.hasNext()) {
            IssueRelation relation = incoming.next();

            if (relation.getRelationType() == IssueRelationType.RELEVANT
                    && relation.getSourceIssue().equals(otherIssue)) {
                incoming.remove();
                relation.getSourceIssue().getRelations().removeOutgoingInternal(relation);
                return relation;
            }
        }
        throw new ResourceNotFoundException(RELATION_NOT_FOUND);
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
     * List of issues that this issue BLOCKS
     */
    public List<Issue> getBlockingIssues() {
        return outgoingRelations.stream()
                .filter(r -> r.getRelationType() == IssueRelationType.BLOCKS)
                .map(IssueRelation::getTargetIssue)
                .toList();
    }

    /**
     * List of issues that BLOCKS this issue
     */
    public List<Issue> getBlockedByIssues() {
        return incomingRelations.stream()
                .filter(r -> r.getRelationType() == IssueRelationType.BLOCKS)
                .map(IssueRelation::getSourceIssue)
                .toList();
    }

    /**
     * List of issues that this issue DUPLICATES
     */
    public List<Issue> getDuplicates() {
        return outgoingRelations.stream()
                .filter(r -> r.getRelationType() == IssueRelationType.DUPLICATES)
                .map(IssueRelation::getTargetIssue)
                .toList();
    }

    /**
     * List of issues that DUPLICATES this issue
     */
    public List<Issue> getDuplicatedBy() {
        return incomingRelations.stream()
                .filter(r -> r.getRelationType() == IssueRelationType.DUPLICATES)
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

    private static void ensureNoRelationExists(Issue source, Issue target) {
        boolean exists = source.getRelations().getOutgoingRelations().stream()
                .anyMatch(relation -> relation.getTargetIssue().equals(target));

        if (exists) {
            throw new BadRequestException(RELATION_ALREADY_EXISTS);
        }
    }

    private void removeIncomingInternal(IssueRelation relation) {
        this.incomingRelations.remove(relation);
    }

    private void removeOutgoingInternal(IssueRelation relation) {
        this.outgoingRelations.remove(relation);
    }
}
