package com.tissue.feature.issue.domain;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issue.domain.exception.IssueSelfReferenceException;
import com.tissue.feature.issue.domain.exception.RelationIssueTypeMismatchException;
import com.tissue.feature.issue.domain.exception.RelationWorkspaceMismatchException;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Table(
        name = "issue_relation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_issue_id", "target_issue_id"}))
@Getter
public class IssueRelation extends HardDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_issue_id", nullable = false)
    private Issue sourceIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_issue_id", nullable = false)
    private Issue targetIssue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueRelationType relationType;

    @SuppressWarnings("NullAway.Init")
    protected IssueRelation() {}

    static IssueRelation create(Issue sourceIssue, Issue targetIssue, IssueRelationType type) {
        ensureSameWorkspace(sourceIssue, targetIssue);
        ensureNotSelfReference(sourceIssue, targetIssue);
        validateRelationType(type, sourceIssue, targetIssue);

        IssueRelation issueRelation = new IssueRelation();
        issueRelation.sourceIssue = sourceIssue;
        issueRelation.targetIssue = targetIssue;
        issueRelation.relationType = type;

        sourceIssue.getRelations().getOutgoingRelations().add(issueRelation);
        targetIssue.getRelations().getIncomingRelations().add(issueRelation);

        return issueRelation;
    }

    private static void ensureNotSelfReference(Issue sourceIssue, Issue targetIssue) {
        if (sourceIssue.equals(targetIssue)) {
            throw new IssueSelfReferenceException(sourceIssue.getWorkspaceKey(), sourceIssue.getKey());
        }
    }

    private static void ensureSameWorkspace(Issue source, Issue target) {
        if (!source.getWorkspaceKey().equals(target.getWorkspaceKey())) {
            throw new RelationWorkspaceMismatchException(
                    source.getWorkspaceKey(), source.getKey(), target.getWorkspaceKey(), target.getKey());
        }
    }

    private static void validateRelationType(IssueRelationType type, Issue sourceIssue, Issue targetIssue) {
        if (type == IssueRelationType.DUPLICATES) {
            boolean issueTypeMismatch = !sourceIssue.getIssueType().equals(targetIssue.getIssueType());
            if (issueTypeMismatch) {
                throw new RelationIssueTypeMismatchException(
                        sourceIssue.getWorkspaceKey(),
                        type,
                        sourceIssue.getKey(),
                        sourceIssue.getIssueType().getName(),
                        targetIssue.getKey(),
                        targetIssue.getIssueType().getName());
            }
        }
    }

    @Override
    public String toString() {
        return String.format(
                "IssueRelation(id=%d, source=%s, target=%s, type=%s)",
                id,
                sourceIssue != null ? sourceIssue.getKey() : "?",
                targetIssue != null ? targetIssue.getKey() : "?",
                relationType);
    }
}
