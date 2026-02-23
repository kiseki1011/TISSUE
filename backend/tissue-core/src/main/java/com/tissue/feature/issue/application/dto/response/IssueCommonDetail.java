package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.application.dto.response.info.IssueTypeInfo;
import com.tissue.feature.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.feature.issue.application.dto.response.info.StateInfo;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.project.domain.ProjectMember;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

// TODO: Consider separating into multiple queries
@Builder
public record IssueCommonDetail(
        Long issueId,
        String issueKey,
        String title,
        @Nullable String content,
        @Nullable String summary,
        IssuePriority priority,
        @Nullable Integer storyPoint,
        @Nullable Instant dueAt,
        @Nullable Instant startedAt,
        @Nullable Instant resolvedAt,

        IssueTypeInfo issueType,
        StateInfo currentState,

        @Nullable Integer countBasedProgress,
        @Nullable Integer pointBasedProgress,

        @Nullable ParticipantInfo author,
        @Nullable ParticipantInfo assignee,
        List<ParticipantInfo> reviewers,

        @Nullable ParticipantInfo lastUpdatedBy,
        Integer subscribersCount,
        Instant createdAt,
        Instant lastUpdatedAt) {

    public static IssueCommonDetail from(
            Issue issue, ProjectMember author, ProjectMember updatedBy, List<IssueReviewer> reviewers) {

        return IssueCommonDetail.builder()
                .issueId(issue.getId())
                .issueKey(issue.getKey())
                .title(issue.getTitle())
                .content(issue.getContent())
                .summary(issue.getSummary())
                .priority(issue.getPriority())
                .storyPoint(issue.getStoryPoint())
                .dueAt(issue.getSchedule().getDueAt())
                .startedAt(issue.getSchedule().getStartedAt())
                .resolvedAt(issue.getSchedule().getResolvedAt())
                .countBasedProgress(issue.getProgress().getCountBasedProgress())
                .pointBasedProgress(issue.getProgress().getPointBasedProgress())
                .issueType(IssueTypeInfo.from(issue.getIssueType()))
                .currentState(StateInfo.from(issue.getCurrentState()))
                .author(ParticipantInfo.from(author))
                .assignee(ParticipantInfo.from(issue.getParticipants().getAssignee()))
                .lastUpdatedBy(ParticipantInfo.from(updatedBy))
                .reviewers(reviewers.stream()
                        .map(IssueReviewer::getReviewer)
                        .map(ParticipantInfo::from)
                        .toList())
                .subscribersCount(issue.getSubscribersCount())
                .createdAt(issue.getCreatedAt())
                .lastUpdatedAt(issue.getLastModifiedAt())
                .build();
    }
}
