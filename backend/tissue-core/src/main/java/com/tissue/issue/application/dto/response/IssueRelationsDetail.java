package com.tissue.issue.application.dto.response;

import com.tissue.issue.application.dto.response.info.RelatedIssueInfo;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueRelation;
import java.util.ArrayList;
import java.util.List;

public record IssueRelationsDetail(
        List<RelatedIssueInfo> blocks,
        List<RelatedIssueInfo> blockedBy,
        List<RelatedIssueInfo> duplicates,
        List<RelatedIssueInfo> duplicatedBy,
        List<RelatedIssueInfo> relevant) {

    public static IssueRelationsDetail from(Issue issue) {
        return new IssueRelationsDetail(
                issue.getRelations().getBlockingIssues().stream()
                        .map(RelatedIssueInfo::from)
                        .toList(),
                issue.getRelations().getBlockedByIssues().stream()
                        .map(RelatedIssueInfo::from)
                        .toList(),
                issue.getRelations().getDuplicates().stream()
                        .map(RelatedIssueInfo::from)
                        .toList(),
                issue.getRelations().getDuplicatedBy().stream()
                        .map(RelatedIssueInfo::from)
                        .toList(),
                issue.getRelations().getRelevantIssues().stream()
                        .map(RelatedIssueInfo::from)
                        .toList());
    }

    public static IssueRelationsDetail from(List<IssueRelation> outgoing, List<IssueRelation> incoming) {
        List<RelatedIssueInfo> blocks = new ArrayList<>();
        List<RelatedIssueInfo> duplicates = new ArrayList<>();
        List<RelatedIssueInfo> relevant = new ArrayList<>();

        for (IssueRelation rel : outgoing) {
            RelatedIssueInfo info = RelatedIssueInfo.from(rel.getTargetIssue());
            switch (rel.getRelationType()) {
                case BLOCKS -> blocks.add(info);
                case DUPLICATES -> duplicates.add(info);
                case RELEVANT -> relevant.add(info);
                default -> {}
            }
        }

        List<RelatedIssueInfo> blockedBy = new ArrayList<>();
        List<RelatedIssueInfo> duplicatedBy = new ArrayList<>();

        for (IssueRelation rel : incoming) {
            RelatedIssueInfo info = RelatedIssueInfo.from(rel.getSourceIssue());
            switch (rel.getRelationType()) {
                case BLOCKS -> blockedBy.add(info);
                case DUPLICATES -> duplicatedBy.add(info);
                case RELEVANT -> relevant.add(info);
                default -> {}
            }
        }

        return new IssueRelationsDetail(blocks, blockedBy, duplicates, duplicatedBy, relevant);
    }
}
