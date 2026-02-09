package com.tissue.issue.application.dto.response.info;

import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.enums.IssuePriority;
import com.tissue.project.domain.ProjectMember;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record IssueBasicInfo(
        String issueKey,
        IssueTypeInfo issueType,
        String title,
        Instant createdAt,
        Instant lastUpdatedAt,
        @Nullable ParticipantInfo author,
        // TODO: is this nullable? i wonder how the lastModifiedBy works in jpa audit
        @Nullable ParticipantInfo lastUpdatedBy,
        @Nullable ParticipantInfo assignee,
        IssuePriority priority,
        StateInfo currentState) {

    public static IssueBasicInfo from(Issue issue, ProjectMember author, ProjectMember lastUpdatedBy) {
        return IssueBasicInfo.builder()
                .issueKey(issue.getKey())
                .issueType(IssueTypeInfo.from(issue.getIssueType()))
                .title(issue.getTitle())
                .createdAt(issue.getCreatedAt())
                .lastUpdatedAt(issue.getLastModifiedAt())
                .author(ParticipantInfo.from(author))
                .lastUpdatedBy(ParticipantInfo.from(lastUpdatedBy))
                .assignee(ParticipantInfo.from(issue.getParticipants().getAssignee()))
                .priority(issue.getPriority())
                .currentState(StateInfo.from(issue.getCurrentState()))
                .build();
    }
}
