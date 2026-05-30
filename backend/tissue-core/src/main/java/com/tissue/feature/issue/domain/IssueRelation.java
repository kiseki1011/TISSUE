package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.ISSUE_SELF_REFERENCE;

import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
        // Relations may span projects (unlike parent links); only self-reference is disallowed.
        ensureNotSelfReference(sourceIssue, targetIssue);

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
            throw new BadRequestException(ISSUE_SELF_REFERENCE);
        }
    }
}
